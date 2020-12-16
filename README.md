# hack-week-generator

![Testing](https://github.com/ELiTLtd/hack-week-generator/workflows/Testing/badge.svg)

Contains clojure source code which comprises the following assets:

- `hack-week-generator-api` - a web service that provides recommendations and representations for learners
- `hack-week-generator-introspect` - a single page application that can be used to interrogate the voila-api
- _`hack-week-generator-microapp` - a (potential) single page application that integrates with Cambridge One_

Currently we host the `hack-week-generator-introspect` web app under `/internal/app` of the
`hack-week-generator-api` although it's a goal of the project to have all three elements be
highly decoupled.

Contains [terraform](https://www.terraform.io/) configuration for managing other
related infrastructure here [`terraform`](terraform).

Contains [GitHub Actions](https://github.com/features/actions) for configuring
[Continuous Integration](#continuous-integration) and [Continuous
Deployment](#continuous-deployment) here
[`.github/workflows`](.github/workflows).

## Required Software & Configuration

 - For development you'll need to be able to clone from/push to github
   [instructions
   here](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/connecting-to-github-with-ssh)

 - For interacting with AWS you'll need to have keys set in your environment
   [instructions
   here](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html)

### MacOS

For developing the `hack-week-generator-api` service:

    brew install leiningen

For developing the `hack-week-generator-introspect` webapp:

    brew install npm
    npm install -g shadow-cljs
    
For building infrastructure:
  
    brew install terraform

For deployment:

    brew install aws/tap/copilot-cli
    brew cask install docker

## Developing

### `hack-week-generator-api`

You should be able to start a development repl with:

    lein repl
    
And run:

    => (start-server 9000)

For the service we're preferring [metosin](https://github.com/metosin) where
they exist for a given problem as they are reliably amongst the most reliable
and offer the most leverage based on the code we write.

Libraries that are useful for understanding the service:

 - [reitit](https://github.com/metosin/reitit) -
   [Documentation](https://cljdoc.org/d/metosin/reitit/0.5.6/doc/introduction)
 - [clojure/spec](https://clojure.org/about/spec) - [Getting
   Started](https://clojure.org/about/spec) - [API
   Docs](https://clojure.github.io/spec.alpha/)

### `hack-week-generator-introspect`

For `hack-week-generator-introspect` we're building our application using
[shadow-cljs](http://shadow-cljs.org/), creating a [react](https://reactjs.org/)
frontend with [reagent](http://reagent-project.github.io/) and taking advantage
of the excellent [evergreen](https://evergreen.segment.com/) component/UI react
framework.

It can be complicated to run all of these parts together _but_ hopefully once
you're up and running you can see the benefit each part brings.

To get started make sure you've installed the dependencies listed above (a bare
minimum would be a working copy of `npm`).

First, make sure we have npm dependencies:

    npm install

Then trigger shadow-cljs to build us an index.js binding:

    npx shadow-cljs watch client

And then in a separate, terminal window run this:

    npx webpack --mode development --watch target/index.js --output public/js/libs.js

## Testing

See the section on [Continuous Integration](#continuous-integration)

### `hack-week-generator-api`

You can run the tests for hack-week-generator-api with

    lein test

## Deploying

_See the section on [Continuous Deployment](#continuous-deployment)_

We use [aws-copilot](https://github.com/aws/copilot-cli) to deploy new versions
of the hack-week-generator-api. It currently depends on building and sending a docker image
based on the current dockerfile in the repository deploying with:

    copilot svc deploy # detailed configuration can be found at `scripts/deploy.sh`

## Infrastructure

We use [aws-copilot](https://github.com/aws/copilot-cli) to manage
infrastructure for running `hack-week-generator-api`. By convention it is running on [AWS
Fargate](https://aws.amazon.com/fargate/) via [AWS
ECS](https://aws.amazon.com/ecs/).

Configuration for the copilot deployment can be found in the `copilot`
directory.

## Continuous Integration

We're using [github actions](https://github.com/features/actions) to build, test
and deploy software.

## Continuous Deployment

When you push to or merge changes into master [an action will automatically
release and deploy](https://github.com/ELiTLtd/hack-week-generator/actions?query=workflow%3ARelease) a new version.
