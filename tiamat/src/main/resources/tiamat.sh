#!/bin/sh

SPARK_HOME=/usr/hdp/2.2.0.0-2041/spark
TIAMAT_HOME=/opt/cloudfeeds-nabu/tiamat

${SPARK_HOME}/bin/spark-submit --class com.rackspace.feeds.archives.Tiamat \
   --files /etc/cloudfeeds-nabu/tiamat/log4j.properties \
   --driver-java-options "-Dlog4j.configuration=file:///etc/cloudfeeds-nabu/tiamat/log4j-local.properties" \
   --master yarn-client ${TIAMAT_HOME}/lib/cloudfeeds-nabu-tiamat.jar "$@"
