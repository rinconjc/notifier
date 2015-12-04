#!/bin/bash

# git hook to deploy code

$(
    cd ~/notifier
    git pull
    pkill -f lein
    ifttt=$(cat .key)
    nohup env JVM_OPTS="-Difttt-key=$ifttt" ./build.sh run &
) >> /var/tmp/git-hook.log
