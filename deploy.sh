#!/usr/bin/env bash
cd "$(dirname "$0")"
USER=$1
if [ -z "$USER" ]
then
  echo "please provide ssh username"
  exit 1
fi
SERVER=$2
if [ -z "$SERVER" ]
then
  echo "please provide server domain or ip address ex. simpatico.mydomain.com"
  exit 1
fi
./prepare-bundle.sh production
ssh $USER@$SERVER 'if [ ! -d cpd-server ]; then echo "creating cpd-server dir..."; mkdir cpd-server; fi'
ssh $USER@$SERVER 'if [ -f "cpd-server/cpd.pid" ]; then echo "stopping remote server..."; ~/cpd-server/stop.sh; fi'
echo "deploying to $SERVER..."
scp -r target/deploy-bundle/* $USER@$SERVER:cpd-server/
echo "starting remote server..."
ssh $USER@$SERVER '~/cpd-server/start.sh'
