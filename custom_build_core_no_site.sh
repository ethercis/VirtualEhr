#!/usr/bin/env bash

cd ethercis-core

echo 'running custom mvn by ignoring failed tests'

mvn clean install -Dmaven.test.failure.ignore=true