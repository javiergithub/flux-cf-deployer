# Flux CloudFoundry Deployer

  This is a prototype for a basic "Flux to CloudFoundry Deployment Service". This Flux service consists
  of two separate processes. You can find them in two separate sub-directories of this repository: 
  
     - flux-cf-deployer: The 'GUI', a web application implemented with Spring-Boot and Java.
     - flux-cf-deployer-service: A headless process that handles requests from the frontend.
     
  The processes communicate with eachother and Flux using the flux message bus. 
  
# Running the Service and WebApp Locally

## Full Localhost deployments

In this setup you run everything (except cloudfoundry itself) locally:

  - flux node.server
  - cf-deployer
  - cf-deployer-service

Steps:

  1. Ensure you have the main flux repo checked out side-by-side with this one.
  2. Start the Flux server locally. See the [Flux readme](https://github.com/eclipse/flux/blob/master/README.md) for instructions.
  3. Make environment variable variable `FLUX_GITHUB_CLIENT_SECRET` is set to the same one used to run the Flux server.
  4. Run the shell script `flux-cf-deployer-releng/run-local.sh`

Notes: 

  - The script runs maven to build both projects and then runs them. To build succesfully you need to have the 
code in the main Flux repo checked out side-by-side with this repo.

  - Two background processes are started. Use OS commands like `kill -9 <pid>` to stop them.
  
  - Make sure not to run more than one instance of `flux-cf-deployer-service` at the same time. The current
    implementation assumes there is only one instance. If two instances are started they will both
    respond to requests simultaneously and this will cause problems.
    
## Running only the cf-deployer app

It is possible to run only the cf-deployer-app locally and consume the cf-deployer-service via 
flux.cfapps.io. 

Steps

 1. edit `cf-deployer/src/main/resources/application.properties'.
    Change cfd.flux.host property to "https://flux.cfapps.io:4443"
 2. Run cf-deployer as a spring-boot app.
 
The cf-deployer app is now running at `http://localhost:8080/`.

This assumes that cf-deployer-service is already running on cloudfoundry and
connected to `https://flux.cfapps.io:4443` as well.

## Running only cf-deployer-service locally

This is also possible but it is not recommended. If you connect a local cf-deployer-service
to flux.cfapps.io and there is already a cf-deployer-service running on CF then both instances
will compete and interfere with eachother causing problems.

## Deploying to CloudFoundry

To deploy to CloudFoundry, run the `flux-cf-deployer-releng/push-all.sh` script.
This requires the 'manifest.yml' files in `flux-cf-deployer` and
`flux-cf-deployer-service`. These files are encrypted with [git-crypt](https://github.com/AGWA/git-crypt).

The files are automatically decrypted on checkout provided you have git-crypt 
properly installed (and configured your repo-clone with the correct encryption key).
