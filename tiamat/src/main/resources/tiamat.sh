#!/bin/sh

SPARK_HOME=/usr/hdp/2.2.0.0-2041/spark
TIAMAT_HOME=/opt/cloudfeeds-nabu/tiamat

${SPARK_HOME}/bin/spark-submit --class com.rackspace.feeds.archives.Tiamat --master yarn-client ${TIAMAT_HOME}/lib/cloudfeeds-nabu-tiamat-*.jar "$@"