#!/usr/bin/env bash

set -x

version=$(lein pprint --no-pretty -- :version)
sed -i -E "s/voila-.+-standalone/voila-$version-standalone/g" Dockerfile
copilot svc deploy --name voila-api --env test --tag "$version"
