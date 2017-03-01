#!/usr/bin/env bash
# replace <MY_OAUTH2_CLIENT_ID> and <MY_OAUTH2_CLIENT_SECRET> with your own oauth2 keys
echo "writing oauth2 keys to config.json..."
sed -i '' -e 's/$OAUTH2_CLIENT_ID/<MY_OAUTH2_CLIENT_ID>/g' target/deploy-bundle/conf/config.json
sed -i '' -e 's/$OAUTH2_CLIENT_SECRET/<MY_OAUTH2_CLIENT_SECRET>/g' target/deploy-bundle/conf/config.json
