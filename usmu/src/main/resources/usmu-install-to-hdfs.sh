#!/bin/sh
#
# This script is used to copy necessary Usmu Coordinator and Workflow files into
# HDFS. Eventually this needs to be part of the deployment scripts (Ansible, etc).
#

# Hive config directory
HIVE_CONFIG_DIR="/etc/hive/conf"

# Need to have some starting assumption of where certain config files are
USMU_ETC_DIR="/etc/cloudfeeds-nabu/usmu"
COORDINATOR_PROPS=${USMU_ETC_DIR}/usmu-coordinator.properties
if [ ! -f ${COORDINATOR_PROPS} ]; then
    echo "ERROR: Coordinator properties file ${COORDINATOR_PROPS} does not exist."
    exit 2
fi
source ${COORDINATOR_PROPS}

# $USMU_HDFS_DIR is defined in $COORDINATOR_PROPS file
hadoop fs -mkdir -p ${USMU_HDFS_DIR}/
hadoop fs -mkdir -p ${USMU_HDFS_DIR}/feedsImport/
hadoop fs -mkdir -p ${USMU_HDFS_DIR}/prefsImport/

# This assumes a certain directory layout as laid out in the RPM.
# The scripts will be installed under $USMU_INSTALL_DIR/bin.
# The rest of the files we need to copy are in the parent dir of bin/
# If you change the RPM, this probably needs to change too.
USMU_INSTALL_DIR=`dirname $0`/..

hadoop fs -copyFromLocal -f ${USMU_INSTALL_DIR}/usmu-coordinator.xml ${USMU_HDFS_DIR}/

# From Oozie coordinator, I cant specify a different workflow xml file
# Instead you can specify directory that contains workflow.xml file

# copying files related to the FeedsImport workflow
hadoop fs -copyFromLocal -f ${HIVE_CONFIG_DIR}/hive-site.xml ${USMU_HDFS_DIR}/feedsImport/
hadoop fs -copyFromLocal -f ${USMU_INSTALL_DIR}/copy_to_entries.q ${USMU_HDFS_DIR}/feedsImport/
hadoop fs -copyFromLocal -f ${USMU_INSTALL_DIR}/feedsImport-wf.xml ${USMU_HDFS_DIR}/feedsImport/workflow.xml

