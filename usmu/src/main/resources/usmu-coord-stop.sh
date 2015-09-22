#!/bin/sh
#
# This script is used to kill existing running Usmu's Oozie Coordinator and Workflow.
#

usage()
{
    # this is just a documentation of how to run this script
    echo "Usage: `basename $0`"
    exit 1
}

# Need to have some starting assumption of where config files are
USMU_ETC_DIR=/etc/cloudfeeds-nabu/usmu
COORDINATOR_PROPS=${USMU_ETC_DIR}/usmu-coordinator.properties

if [ ! -f ${COORDINATOR_PROPS} ]; then
    echo "ERROR: Coordinator properties file ${COORDINATOR_PROPS} does not exist."
    exit 2
fi
source ${COORDINATOR_PROPS}

# Clean up previous run. 
for region in $REGION_LIST
do
    jobId=`oozie jobs -oozie ${OOZIE_URL} -jobtype coordinator -filter "status=RUNNING;name=FeedsImport-$region" | grep FeedsImport-$region | awk '{print $1}'`
    if [ "$jobId" != "" ]; then
        echo "Stopping/kill job $jobId"
        oozie job -oozie ${OOZIE_URL} -kill $jobId
    fi
done
exit 0
