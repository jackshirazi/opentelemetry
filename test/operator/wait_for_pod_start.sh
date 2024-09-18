#!/bin/bash

set -euxo pipefail

MAX_WAIT_SECONDS=60
NAMESPACE=$1
GREP=$2

echo "Waiting an initial 10 seconds for the $NAMESPACE pods to start"
sleep 10
echo "Waiting up to $MAX_WAIT_SECONDS more seconds for the $NAMESPACE pods to be ready"
count=0
while [ $count -lt $MAX_WAIT_SECONDS ]
do
  count=`expr $count + 1`
  STARTED=$(kubectl get pod -n $NAMESPACE | (grep "$GREP" || true) | wc -l)
  if [ $STARTED -eq 0 ]
  then
    exit 0
  fi
  sleep 1
done

echo "error: the $NAMESPACE pods failed to be ready within $MAX_WAIT_SECONDS seconds"
echo "-- pod info:"
kubectl get pod -A
echo "--"
exit 1
