#!/usr/bin/env bash
PILOT=$1
if ! [[ "$PILOT" =~ ^(trento|galicia|sheffield|all)$ ]]; then
  echo "please provide a pilot ID (trento|galicia|sheffield)"
  exit 1
fi

COLLECTIONS="properties schemas models dis extensions"

for COLLECTION in $COLLECTIONS; do
  mongodump --gzip --db=cpd --collection=$COLLECTION --out="dump/$PILOT"
done

SRC=$2
if [[ $PILOT == "all" ]] && [[ $SRC ]] && [[ $SRC == "src" ]]; then
  echo "exporting jsons to src..."
  for COLLECTION in $COLLECTIONS; do
    mongoexport --db=cpd --collection=$COLLECTION --jsonArray --pretty --out src/main/deploy-bundle/data/db/collections/$COLLECTION.json
  done
fi