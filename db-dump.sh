#!/usr/bin/env bash
PILOT=$1
if ! [[ "$PILOT" =~ ^(trento|galicia|sheffield|all)$ ]]; then
  echo "please provide a pilot ID (trento|galicia|sheffield)"
  exit 1
fi
mongodump --out "dump/$PILOT" --db cpd --gzip
SRC=$2
if [[ $PILOT == "all" ]] && [[ $SRC ]] && [[ $SRC == "src" ]]; then
    echo "exporting jsons to src..."
    mongoexport --db cpd --collection dis --jsonArray --pretty --out src/main/deploy-bundle/data/db/dis.json
    mongoexport --db cpd --collection models --jsonArray --pretty --out src/main/deploy-bundle/data/db/models.json
    mongoexport --db cpd --collection schemas --jsonArray --pretty --out src/main/deploy-bundle/data/db/schemas.json
    mongoexport --db cpd --collection users --jsonArray --pretty --out src/main/deploy-bundle/data/db/users.json
    mongoexport --db cpd --collection user.feedbacks --jsonArray --pretty --out src/main/deploy-bundle/data/db/user.feedbacks.json
fi