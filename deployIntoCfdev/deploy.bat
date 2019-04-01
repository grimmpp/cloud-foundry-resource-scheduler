
## create mysql
cf create-service p-mysql 10mb mysql

## push app
cf push resourceScheduler -p "..\build\libs\resourceScheduler-0.0.1-SNAPSHOT.war" -f resourceScheduler_manifest.yml


## register service broker
cf create-service-broker resourceSchedulerBroker admin admin https://resourceScheduler.dev.cfdev.sh
#cf update-service-broker resourceSchedulerBroker admin admin https://resourceScheduler.dev.cfdev.sh
cf enable-service-access "Resource Scheduler"

## Create Instance of AppRestarter service plan
## Create service instance of restarter plan
cf create-service "Resource Scheduler" "AppRestarter" appRestarter1
## crate binding to existing test app
cf push test-app1 -p .\test-app  -m 20M -k 20M -b staticfile_buildpack
cf bind-service test-app1 appRestarter1 -c '{\"time\":\"2m\"}'


## Create Instance of SwitchOffAppsInSpace
cf create-space switchOffAllApps
cf target -o cfdev-org -s switchOffAllApps
cf push test-app2 -p .\test-app  -m 20M -k 20M -b staticfile_buildpack
cf create-service "Resource Scheduler" "SwitchOffAppsInSpace" switchOffAppsAfter2min -c '{\"time\":\"2m\"}'

## Create Instance of HttpEndpointScheduler
cf create-service "Resource Scheduler" "HttpEndpointScheduler" httpEPsi -c '{\"time\":\"10s\", \"url\":\"https://test-app1.dev.cfdev.sh\", \"sslEnabled\": false}'


# cleanup
# cf unbind-service test-app appRestarter1
# cf delete test-app1
# cf delete-service appRestarter1

# cf delete test-app2
# cf delete-service switchOffAppsAfter2min

# cf delete-service httpEPsi

# cf delete-service-broker appManager