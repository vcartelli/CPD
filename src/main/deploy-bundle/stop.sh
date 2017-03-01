#!/bin/bash
cd "$(dirname "$0")"
if [ -f cpd.pid ]; then
  echo "cpd-server id going to be killed..."
  kill $(cat cpd.pid)
  rm cpd.pid
else
  echo "pid file not found, is the server running?"
fi