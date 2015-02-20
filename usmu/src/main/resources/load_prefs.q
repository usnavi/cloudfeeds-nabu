--
-- This is the Hive script that is to be launched when
-- the Preferences Service Postgres dump process is completed 
-- every day, signaled by the existence of _SUCCESS file
-- in an agreed HDFS directory.
-- 
SET hive.exec.dynamic.partition=true;
SET hive.exec.dynamic.partition.mode=nonstrict;

-- Drop the external table pointing to previous
-- run directory location
DROP TABLE IF EXISTS ${INPUT_TABLE};

-- Create an external table pointing to current
-- run directory location
CREATE external TABLE IF NOT EXISTS ${INPUT_TABLE} (
       id string,
       alternate_id string,
       created timestamp,
       updated timestamp,
       payload string)
LOCATION '${INPUT_LOCATION}';

