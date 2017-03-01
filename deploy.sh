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
KEYSTORE_PATH=$3
if [ -z "$KEYSTORE_PATH" ]
then
  echo "please provide keystore's full path for $SERVER"
  exit 1
fi
echo "starting production compilation..."
# create your own oauth.sh from oauth-example.sh
mvn clean package -P production; ./keystore.sh; ./oauth.sh;
echo "copying target to deploy-bundle..."
cp target/cpd-server-1.0-SNAPSHOT-fat.jar target/deploy-bundle/cpd-server.jar
echo "copying keystore to deploy-bundle..."
cp $KEYSTORE_PATH target/deploy-bundle/
chmod 754 target/deploy-bundle/*.sh
echo "creating cpd-server dir..."
ssh $USER@$SERVER 'if [ ! -d cpd-server ]; then mkdir cpd-server; fi'
echo "stopping remote server..."
ssh $USER@$SERVER '~/cpd-server/stop.sh'
echo "deploying to $SERVER..."
scp -r target/deploy-bundle/* $USER@$SERVER:cpd-server/
echo "starting remote server..."
ssh $USER@$SERVER '~/cpd-server/start.sh'