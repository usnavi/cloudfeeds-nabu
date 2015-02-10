#!/bin/sh
#
# This script is used to deploy new versions of Usmu's Oozie Coordinator and Workflow.

# TODO: this probably needs to be externalized somewhere
USMU_DIR=/etc/cloudfeeds-nabu/usmu
REGION_LIST="DFW ORD IAD SYD LON HKG"
OOZIE=oozie
OOZIE_URL=http://cloudfeeds-visual-n02.test.ord1.ci.rackspace.net:11000/oozie
ERROR_EMAIL=cloudfeeds@rackspace.com

# There doesn't seem to be a nicer way to find existing Oozie jobs so
# we can kill and submit a new set. This one I found on the web and
# probably needs to be evaluated every time we upgrade Oozie.

# clean up previous run
curl '${OOZIE_URL}/v1/jobs?len=1000&filter=status%3DRUNNING&jobtype=coord'  | python -mjson.tool | grep "coordJobId" | sed "s/\(.*\)coordJobId\(.*\): \"\(.*\)\"\(.*\)/\3/" | while read job_id; do oozie job -oozie ${OOZIE_URL} -kill $job_id; done

# run the feedsImport-coord.xml for each region
for aRegion in ${REGION_LIST}
do
    $OOZIE job -oozie ${OOZIE_URL} -config ${USMU_DIR}/feedsImport-coord.properties -submit -Dregion=$aRegion -DemailToAddress="$ERROR_EMAIL" -DstartTime="2015-02-04T00:00Z" -DendTime="2025-12-31T00:00Z"
done
