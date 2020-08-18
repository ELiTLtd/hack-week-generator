#!/usr/bin/env bash

set -x

version=$1

lein do clean, uberjar
sed -i -E "s/voila-.+-standalone/voila-${version//\"/}-standalone/g" Dockerfile
copilot svc deploy --name voila-api --env test --tag $version
