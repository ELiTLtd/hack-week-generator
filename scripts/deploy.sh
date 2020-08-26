#!/usr/bin/env bash

copilot svc deploy --name voila-api --env test --tag $(lein pprint --no-pretty -- :version)
