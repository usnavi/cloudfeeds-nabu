-- This is the DDL of the preferences table in Hive that
-- stores all of our customers' Cloud Feeds Archive 
-- preferences.
--
-- This MUST match with how the Spark job is reading
-- the table and how Ballista is writing the files
--
-- Parameter:
--   INPUT_LOCATION: 
--       the directory where Ballista will be dumping files
--
-- To create the ddl in Hive, run the following:
--   hive -f prefs.hive.ddl -d INPUT_LOCATION=/cloudfeeds/prefs_dump
--

DROP TABLE IF EXISTS preferences;

CREATE external TABLE IF NOT EXISTS preferences (
       id string,
       alternate_id string,
       enabled boolean,
       payload string,
       created timestamp,
       updated timestamp)
LOCATION '${INPUT_LOCATION}';
