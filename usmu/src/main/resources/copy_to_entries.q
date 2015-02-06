
SET hive.exec.dynamic.partition=true;
SET hive.exec.dynamic.partition.mode=nonstrict;

CREATE external TABLE IF NOT EXISTS ${INPUT_TABLE} (
       id bigint,
       entryid string,
       creationdate timestamp,
       datelastupdated timestamp,
       entrybody string,
       categories string,
       eventtype string,
       tenantid string,
       dc string,
       feed string,
       date string)
LOCATION '${INPUT_LOCATION}';

FROM ${INPUT_TABLE} ent
INSERT OVERWRITE TABLE entries_orc PARTITION(feed, date)
SELECT ent.id, ent.entryid, ent.creationdate, ent.datelastupdated, ent.entrybody, ent.categories,
ent.eventtype, ent.tenantid, ent.dc, ent.feed, ent.date;

DROP TABLE ${INPUT_TABLE};
