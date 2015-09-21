#!/bin/sh
 
TIAMAT_HOME=/opt/cloudfeeds-nabu/tiamat
 
/usr/bin/spark-submit --verbose --class com.rackspace.feeds.archives.Tiamat \
   --files /etc/cloudfeeds-nabu/tiamat/log4j.properties \
   --driver-java-options "-Dlog4j.configuration=file:///etc/cloudfeeds-nabu/tiamat/log4j-local.properties" \
   --master yarn-client ${TIAMAT_HOME}/lib/cloudfeeds-nabu-tiamat.jar "$@"