#!/bin/bash
cd "$(dirname "$0")"
docker run -v "$PWD"/mongo-persistent:/var/lib/mongodb -v "$PWD"/config-persistent:/deploy/cpd-configuration  -p 8901:8901 -i -t cpd-deploy-ubuntu:latest /bin/bash
