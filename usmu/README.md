Usmu
====
Usmu's artifacts are simply a set of XML files to be installed on HDFS and submitted to Oozie.

## How to Build
Usmu is built as part of cloudfeeds-nabu. To build it, we require:
* Gradle version 2.2 or above

### Simple build
```
gradle clean build
```

### Build an RPM
```
gradle clean buildRpm
```

## How to Install Usmu
Usmu runs on the Gateway node or on the Name node of a Hadoop cluster where Oozie is installed. 

Here are the steps to install Usmu:

1. Log on to the Gateway node or the Name node of your Hadoop cluster.
```
rpm -Uvh cloudfeeds-nabu-usmu-${version}-1.noarch.rpm
```
where ```${version}``` is the version of Usmu you want to install
1. Run the script to copy files to HDFS
```
/opt/cloudfeeds-nabu/usmu/bin/usmu-install-to-hdfs.sh
```
1. If this is the first time you install Usmu in your Hadoop cluster, run the following Hive scripts:
```
hive -f entries.hive.ddl
hive -f prefs.hive.ddl -d INPUT_LOCATION=${prefsDumpDir}
```
where ```${prefsDumpDir}``` is the directory where Ballista writes the Postgres DB dump of the 
Preferences Service database.
1. Run the script to kill old Usmu Oozie jobs and submit the new ones
```
/opt/cloudfeeds-nabu/usmu/bin/usmu-coord-run.sh
```
