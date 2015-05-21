#!/bin/sh

# get current time in format H(0..23)MM(00..59)
now=$(date +%k%M)

# exit unless current time is in the right window
if [ $now -lt 1000 -o $now -gt 1400 ]; then
    echo 0
    exit 0
fi

# get yesterday date in YYYY-MM-DD format
yesterday=`date -d "1 day ago" +%Y-%m-%d`

# path to _SUCCESS file in hadoop filesystem
success_file="/user/cloudfeeds/prefs_dump/_$yesterday/_SUCCESS"

# test for file
hadoop fs -test -e $success_file
rc=$?

if [ $rc -eq 0 ]; then
    # SUCCESS
    echo 0
    exit 0
else
    # FAILURE
    echo 1
    exit 1
fi
