# voila

![Testing](https://github.com/ELiTLtd/voila/workflows/Testing/badge.svg)

Contains clojure source code which comprises the following assets:

- `voila-api` - a web service that provides recommendations and representations for learners
- `voila-introspect` - a single page application that can be used to interrogate the voila-api
- _`voila-microapp` - a (potential) single page application that integrates with Cambridge One_

Currently we host the `voila-introspect` web app under `/internal/app` of the
`voila-api` although it's a goal of the project to have all three elements be
highly decoupled.

Contains [terraform](https://www.terraform.io/) configuration for managing other
related infrastructure here [`terraform`](terraform).

Contains [GitHub Actions](https://github.com/features/actions) for configuring
[Continuous Integration](#continuous-integration) and [Continuous
Deployment](#continuous-deployment) here
[`.github/workflows`](.github/workflows).

## Required Software & Configuration

### GitHub Access Key

### AWS Credentials

### MacOS

- `voila-api`:

    brew install leiningen
    brew install aws/tap/copilot-cli
    brew cask install docker

- `voila-introspect`:

    brew install npm
    npm install -g shadow-cljs
    
- `terraform`:
  
    npm install terraform

## Developing

### `voila-api`

You should be able to start a development repl with:

    lein repl
    
And run:

    => (start-server 9000)

## Testing

See the section on [Continuous Integration](#continuous-integration)

### `voila-api`

You can run the tests for voila-api with

    lein test

## Deploying

_See the section on [Continuous Deployment](#continuous-deployment)_

We use [aws-copilot](https://github.com/aws/copilot-cli) to deploy new versions
of the voila-api. It currently depends on building and sending a docker image
based on the current dockerfile in the repository deploying with:

  copilot svc deploy # detailed configuration can be found at `scripts/deploy.sh`

## Infrastructure

We use [aws-copilot](https://github.com/aws/copilot-cli) to manage
infrastructure for running `voila-api`. By convention it is running on [AWS
Fargate](https://aws.amazon.com/fargate/) via [AWS
ECS](https://aws.amazon.com/ecs/).

Configuration for the copilot deployment can be found in the `copilot`
directory.

External dependencies 

## Continuous Integration

_TBD_

## Continuous Deployment

_TBD_
