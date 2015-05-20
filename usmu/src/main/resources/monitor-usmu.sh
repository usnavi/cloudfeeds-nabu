#!/bin/sh

usage()
{
    echo "Usage: $0 <region>"
    echo "where"
    echo "  <region> is one of ORD, DFW, IAD, LON, SYD, HKG"
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi
region=$1

# get current time in format H(0..23)MM(00..59)
now=$(date +%k%M)

# exit unless current time is in the right window
if [ $now -lt 1400 -o $now -gt 1800 ]; then
    echo "Time is not between 2:00 PM UTC and 6:00 PM UTC. Skipping checks."
    echo 0
    exit 0
fi

# get yesterday date in YYYY-MM-DD format
yesterday=`date -d "1 day ago" +%Y-%m-%d`

# path to success.txt file in hadoop filesystem
success_file="/user/cloudfeeds/cloudfeeds-nabu/usmu/run/$region/$yesterday/success.txt"

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
