#!/bin/bash

# git hook to deploy code

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
git pull
pkill -f lein
ifttt=$(cat .key)
nohup env JVM_OPTS="-Difttt-key=$ifttt" ./build.sh run > ./run.log &
