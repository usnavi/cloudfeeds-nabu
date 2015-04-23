#!/bin/sh
#
# This script is used to deploy new versions of Usmu's Oozie Coordinator and Workflow.
# It is a temporary script used during development, when we're updating coordinators
# and workflows. This eventually needs to be part of the deployment script (Ansible,
# etc)
#

usage()
{
    echo "Usage: `basename $0` <startTime> <endTime>"
    echo "where:"
    echo "  <startTime>   is the start time of when Oozie should materialize jobs. Format: yyyy-MM-ddThh:mmZ"
    echo "  <endTime>     is the end time of when Oozie should stop materializing jobs. Same format as above"
    exit 1
}

if [ $# -lt 2 ]; then
    echo "Missing argument to script"
    usage
fi

START_TIME=$1
END_TIME=$2

# Need to have some starting assumption of where config files are
USMU_ETC_DIR=/etc/cloudfeeds-nabu/usmu
COORDINATOR_PROPS=${USMU_ETC_DIR}/usmu-coordinator.properties

if [ ! -f ${COORDINATOR_PROPS} ]; then
    echo "ERROR: Coordinator properties file ${COORDINATOR_PROPS} does not exist."
    exit 2
fi
source ${COORDINATOR_PROPS}

# There doesn't seem to be a nicer way to find existing Oozie jobs so
# we can kill and submit a new set. This one I found on the web and
# probably needs to be evaluated every time we upgrade Oozie.

# Clean up previous run. 
# WARNING: this WILL delete *all* RUNNING jobs.
curl ${OOZIE_URL}'/v1/jobs?len=1000&filter=status%3DRUNNING&jobtype=coord'  | python -mjson.tool | grep "coordJobId" | sed "s/\(.*\)coordJobId\(.*\): \"\(.*\)\"\(.*\)/\3/" | while read job_id; do oozie job -oozie ${OOZIE_URL} -kill $job_id; done

# run the usmu-coordinator.xml for each region to watch for Cloud Feeds dump
for aRegion in ${REGION_LIST}
do
    $OOZIE job -oozie ${OOZIE_URL} -config ${COORDINATOR_PROPS} -submit \
        -Doozie.coord.application.path=${nameNode}${USMU_HDFS_DIR}/usmu-coordinator.xml \
        -DcoordAppName=FeedsImport-${aRegion} \
        -DwatchDir=${FEEDS_DUMP_DIR}/${aRegion} \
        -DworkflowPath=${USMU_HDFS_DIR}/feedsImport \
        -DsuccessDir=${USMU_SUCCESS_DIR} \
        -Dregion=$aRegion -DemailToAddress="${ERROR_EMAIL}" \
        -DstartTime="${START_TIME}" -DendTime="${END_TIME}"
done
