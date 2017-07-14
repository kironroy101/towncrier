#!/bin/sh -e

if [ "$1" = "purge" ] ; then
  update-rc.d towncrier remove >/dev/null
fi
