#!/bin/bash
cd "$(dirname "$0")"
if [ ! -d log ]; then
    mkdir log
fi
if [ ! -f cpd.pid ]; then
    echo "cpd-server is going to start..."
    nohup java -jar cpd-server.jar > log/cpd.log 2>&1 & echo $! > cpd.pid
else
  echo "pid file found, is the server already running? (use stop.sh and the run start.sh again)"
  echo "PID: $(cat cpd.pid)"
fi