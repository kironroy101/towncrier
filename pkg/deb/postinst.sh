#!/bin/sh -e
# Fakeroot and lein don't get along, so we set ownership after the fact.
chown -R root:root /usr/share/towncrier
chown root:root /usr/bin/towncrier
chown towncrier:towncrier /var/log/towncrier
chown -R towncrier:towncrier /etc/towncrier
chown root:root /etc/init.d/towncrier
chown root:root /etc/default/towncrier

# Start towncrier on boot
if [ -x "/etc/init.d/towncrier" ]; then
  if [ ! -e "/etc/init/towncrier.yml" ]; then
    update-rc.d towncrier defaults >/dev/null
  fi
fi

invoke-rc.d towncrier start
