-- This is the DDL of the entries table in Hive that
-- stores all Cloud Feeds entries.
--
-- This MUST match with how the Spark job is reading
-- the table.

CREATE TABLE entries(
       id bigint,
       entryid string,
       creationdate timestamp,
       datelastupdated timestamp,
       entrybody string,
       categories string,
       eventtype string,
       tenantid string,
       region string)
PARTITIONED BY( date string, feed string );
-- NOTE:  once we move to Hive .14, we'll update the format as PARQUET.  Spark does not
-- yet handle ORC files with timestamp, nor does it handle ORC very efficiently.
--STORED as PARQUET;
