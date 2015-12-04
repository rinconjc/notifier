#!/bin/bash

# git hook to deploy code
set -e
cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "cur dir: $PWD"
git pull
pkill -f lein
ifttt=$(cat .key)
env JVM_OPTS="-Difttt-key=$ifttt" ./build.sh run > ./run.log &
