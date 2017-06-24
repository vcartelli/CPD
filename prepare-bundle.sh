#!/usr/bin/env bash
PROFILE=$1
if [ -z "$PROFILE" ]; then
  PROFILE=production
fi
if [ "$PROFILE" != "develop" ] && [ "$PROFILE" != "production" ]; then
  echo "unknown profile '$PROFILE', it should be 'develop' or 'production'"
  exit 1
fi;
cd "$(dirname "$0")"
echo "starting $PROFILE compilation..."
mvn clean package -P $PROFILE
echo "copying target jar to deploy-bundle..."
cp target/cpd-server-1.2-SNAPSHOT-fat.jar target/deploy-bundle/cpd-server.jar
chmod 754 target/deploy-bundle/*.sh
