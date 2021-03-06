#!/bin/bash
set -e
cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "cur dir: $PWD"
git pull

pkill -f lein | echo
ifttt=$(cat .key)
env JVM_OPTS="-Difttt-key=$ifttt" ./build.sh run > ./run.log 2>&1 &

echo "deployed!"
