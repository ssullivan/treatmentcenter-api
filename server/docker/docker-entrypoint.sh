#!/bin/sh
set -e

if [[ -n "$GID" ]]; then
  groupmod -o -g $GID app_user
fi

if [[ -n "$UID" ]]; then
  usermod -o -u $UID app_user
fi

if [ "$1" = 'app_user' -a "$(id -u)" = '0' ]; then
  chown -R app_user /data
  exec su-exec app_user "$@"
fi

exec "$@"

