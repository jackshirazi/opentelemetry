#!/bin/bash

set -euxo pipefail

MAX_WAIT_SECONDS=60
NAMESPACE=$1
GREP=$2

echo "Waiting up to $MAX_WAIT_SECONDS seconds for the cert managers to start"
count=0
while [ $count -lt $MAX_WAIT_SECONDS ]
do
  count=`expr $count + 1`
  STARTED=`kubectl get pod -n $NAMESPACE | grep "$GREP"`
  if [ "x$STARTED" = "x" ]
  then
    exit 0
  fi
  sleep 1
done

echo "error: cert managers failed to start within $MAX_WAIT_SECONDS seconds"
echo "-- pod info:"
kubectl get pod -A
echo "--"
exit 1
