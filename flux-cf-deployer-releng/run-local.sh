#!/bin/bash
set -e

basedir=`pwd`/..
echo "==== Running maven build=====================" 
mvn clean install

echo "==== starting: flux-cf-deployer-service ===="
echo "Logs are in " `pwd`/service.log
cd ${basedir}/flux-cf-deployer-service
java -jar target/org.eclipse.flux.cloudfoundry.deployment.service-*-jar-with-dependencies.jar > service.log &

echo "==== deploying: flux-cf-deployer ============"
cd ${basedir}/flux-cf-deployer
echo "Logs are in " `pwd`/webapp.log
java -jar target/flux-cf-deployer-*.jar > webapp.log

echo "==== DONE ===================================="

