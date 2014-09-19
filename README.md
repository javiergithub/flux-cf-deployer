# Flux CloudFoundry Deployer

  This is a prototype for a basic "Flux to CloudFoundry Deployment Service". This Flux service consists
  of two separate processes. You can find them in two separate sub-directories of this repository: 
  
     - flux-cf-deployer: The 'GUI', a web application implemented with Spring-Boot and Java.
     - flux-cf-deployer-service: A headless process that handles requests from the frontend.
     
  The processes communicate with eachother and Flux using the flux message bus. 
  
# Running the Service and WebApp Locally

## Localhost deployments

First you must make sure to have Flux server running locally as well. See the Flux readme
for instructions.

When you have Flux up and running, run the shell script in `flux-cf-deployer-releng/run-local.sh`.

This script runs maven to build both projects and then runs them. 
Note that to build it succesfully you need to have the code in the main Flux repo checked out
side-by-side with this repo.

## Deploying to CloudFoundry

To deploy to CloudFoundry, run the `flux-cf-deployer-releng/push-all.sh` script.
Note that thus requires the 'manifest.yml' files in `flux-cf-deployer` and
`flux-cf-deployer-service`. These files are encrypted with [git-crypt](https://github.com/AGWA/git-crypt).

The files are automatically decrypted on checkout provided you have git-crypt 
properly installed (and configured your repo-clone with the correct encryption key).
