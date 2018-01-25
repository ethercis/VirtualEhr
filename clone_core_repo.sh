#!/usr/bin/env bash

git init ethercis-core
cd ethercis-core/
git remote add origin https://github.com/serefarikan/ehrservice.git > /dev/null 2>&1

#use a shallow clone to minimise traffic
#TODO: trigger a script to archive contents of this repo and reset
#contents to a single entry
git pull --depth=1 origin master