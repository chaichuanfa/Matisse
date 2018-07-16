#!/usr/bin/env bash

./gradlew clean --offline
if [ $? -eq 0 ];then
    ./gradlew assembleRelease --offline
    if [ $? -eq 0 ];then
        ./gradlew artifactoryPublish --offline
    fi
fi

