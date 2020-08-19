#!/usr/bin/env bash

set -x

copilot svc deploy --name voila-api --env test --tag $(lein pprint --no-pretty -- :version)
