#!/bin/sh

HDFS_DEPLOY_DIR=/cloudfeeds/usmu
#USMU_LOCAL_DIR=/etc/cloudfeeds-nabu/usmu
USMU_LOCAL_DIR=./

hadoop fs -mkdir -p ${HDFS_DEPLOY_DIR}/

# this should copy 
hadoop fs -copyFromLocal -f ${USMU_LOCAL_DIR}/*.xml ${USMU_LOCAL_DIR}/*.q ${HDFS_DEPLOY_DIR}/
