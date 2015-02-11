#!/bin/sh
#
# This script is used to copy necessary Usmu Coordinator and Workflow files into
# HDFS. Eventually this needs to be part of the deployment scripts (Ansible, etc).
# 

HDFS_DEPLOY_DIR=/cloudfeeds/usmu
USMU_INSTALL_DIR=`dirname $0`/..

hadoop fs -mkdir -p ${HDFS_DEPLOY_DIR}/

#
# This assumes a certain directory layout as laid out in the RPM.
# The scripts will be installed under $USMU_INSTALL_DIR/bin.
# The necessary files are installed directly under $USMU_INSTALL_DIR.
# If you change the RPM, this probably needs to change too.
#
hadoop fs -copyFromLocal -f ${USMU_INSTALL_DIR}/*.xml ${USMU_INSTALL_DIR}/*.q ${HDFS_DEPLOY_DIR}/
