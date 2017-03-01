#!/usr/bin/env bash
# replace <MY_KEYSTORE_FILENAME> and <MY_KEYSTORE_PASSWORD> with your own keystore filename and password
echo "writing keystore settings to config.json..."
sed -i '' -e 's/$KEYSTORE_FILENAME/<MY_KEYSTORE_FILENAME>/g' target/deploy-bundle/conf/config.json
sed -i '' -e 's/$KEYSTORE_PASSWORD/<MY_KEYSTORE_PASSWORD>/g' target/deploy-bundle/conf/config.json
