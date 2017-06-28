#!/usr/bin/env bash
PILOT=$1
if ! [[ "$PILOT" =~ ^(trento|galicia|sheffield|all)$ ]]
then
  echo "please provide a pilot ID (trento|galicia|sheffield)"
  exit 1
fi
mongodump --out "dump/$PILOT" --db cpd --gzip
SRC=$2
if [ $PILOT == "all" ] && [ $SRC == "src" ]
then
  mongoexport --db cpd --collection schema --jsonArray --pretty --out src/main/deploy-bundle/data/db/schema.json
  mongoexport --db cpd --collection model --jsonArray --pretty --out src/main/deploy-bundle/data/db/model.json
  mongoexport --db cpd --collection diagram --jsonArray --pretty --out src/main/deploy-bundle/data/db/diagram.json
fi