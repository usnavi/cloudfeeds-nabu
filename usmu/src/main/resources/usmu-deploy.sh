#!/bin/sh
#
# This script is used to copy necessary Usmu Coordinator and Workflow files into
# HDFS

HDFS_DEPLOY_DIR=/cloudfeeds/usmu
USMU_LOCAL_DIR=/opt/cloudfeeds-nabu/usmu

hadoop fs -mkdir -p ${HDFS_DEPLOY_DIR}/

# this should copy 
hadoop fs -copyFromLocal -f ${USMU_LOCAL_DIR}/*.xml ${USMU_LOCAL_DIR}/*.q ${HDFS_DEPLOY_DIR}/
