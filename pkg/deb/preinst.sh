#!/bin/sh -e
# Create trawler user and group
USERNAME="towncrier"
GROUPNAME="towncrier"
getent group "$GROUPNAME" >/dev/null || groupadd -r "$GROUPNAME"
getent passwd "$USERNAME" >/dev/null || \
  useradd -r -g "$GROUPNAME" -d /usr/share/towncrier -s /bin/false \
  -c "Towncrier alert system" "$USERNAME" 
exit 0
