#!/bin/sh
USERNAME=cloudfeeds
GROUP=cloudfeeds
HOME_DIR=/opt/cloudfeeds-nabu
getent group $GROUP >/dev/null || groupadd -r $GROUP
getent passwd $USERNAME >/dev/null || useradd -r -g $USERNAME -s /sbin/nologin -d $HOME_DIR -c "Rackspace Cloud Feeds Archiving Service" $USERNAME
