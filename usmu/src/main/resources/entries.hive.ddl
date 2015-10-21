-- This is the DDL of the entries table in Hive that
-- stores all Cloud Feeds entries.
--
-- This MUST match with how the Spark job is reading
-- the table.

SET hive.support.sql11.reserved.keywords=false;

CREATE TABLE IF NOT EXISTS entries(
       id bigint,
       entryid string,
       creationdate timestamp,
       datelastupdated timestamp,
       entrybody string,
       categories string,
       eventtype string,
       tenantid string,
       region string)
PARTITIONED BY( date string, feed string )
STORED as ORC;
