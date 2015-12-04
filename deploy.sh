#!/bin/bash

# git hook to deploy code

cd ~/notifier
pkill -f lein
ifttt=$(cat .key)
nohup env JVM_OPTS="-Difttt-key=$ifttt" ./build.sh run &
