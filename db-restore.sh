#!/usr/bin/env bash
PILOT=$1
if ! [[ "$PILOT" =~ ^(trento|galicia|sheffield|all)$ ]]
then
  echo "please provide a pilot ID (trento|galicia|sheffield)"
  exit 1
fi
mongorestore --dir "dump/$PILOT" --nsInclude 'cpd.*' --drop --gzip