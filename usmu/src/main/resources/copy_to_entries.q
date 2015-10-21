--
-- This is the Hive script that is to be launched when
-- the Cloud Feeds Postgres dump process is completed 
-- every day, signaled by the existence of _SUCCESS file
-- in an agreed HDFS directory
-- 
SET hive.exec.dynamic.partition=true;
SET hive.exec.dynamic.partition.mode=nonstrict;
SET hive.support.concurrency=true;
SET hive.support.sql11.reserved.keywords=false;

-- Create an external table pointing to an agreed HDFS
-- directory where Postgres DB dumb is written.
-- This schema MUST match with what Postgres DB dump process
-- is exporting.
DROP TABLE IF EXISTS ${INPUT_TABLE};

CREATE external TABLE IF NOT EXISTS ${INPUT_TABLE} (
       id bigint,
       entryid string,
       creationdate timestamp,
       datelastupdated timestamp,
       entrybody string,
       categories string,
       eventtype string,
       tenantid string,
       region string,
       date string,
       feed string)
LOCATION '${INPUT_LOCATION}';

-- Read from external table and insert into a partitioned Hive table
FROM ${INPUT_TABLE} ent
INSERT OVERWRITE TABLE entries PARTITION(date, feed)
SELECT ent.id, ent.entryid, ent.creationdate, ent.datelastupdated, ent.entrybody, ent.categories,
ent.eventtype, ent.tenantid, ent.region, ent.date, ent.feed;
