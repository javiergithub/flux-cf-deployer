#!/bin/bash
set -e

# Set these variables in your .bashrc script or some other
# convenient place where you can define these on your local 
# machine:
#
#FLUX_GITHUB_CLIENT_ID=...get this from github...
#FLUX_GITHUB_CLIENT_SECRET=...get this from gihub...
#
#They should contain the same information / tokens as the ones you
#are using the run the local flux server.

basedir=`pwd`/..
echo "==== Running maven build=====================" 
mvn clean install

echo "==== starting: flux-cf-deployer-service ===="
echo "Logs are in " `pwd`/service.log
cd ${basedir}/flux-cf-deployer-service
java -Dflux-token=${FLUX_GITHUB_CLIENT_SECRET} -jar target/org.eclipse.flux.cloudfoundry.deployment.service-*-jar-with-dependencies.jar > service.log &

echo "==== deploying: flux-cf-deployer ============"
cd ${basedir}/flux-cf-deployer
echo "Logs are in " `pwd`/webapp.log
java -jar target/flux-cf-deployer-*.jar > webapp.log &

echo "==== DONE ===================================="

