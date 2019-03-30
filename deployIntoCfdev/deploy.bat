
cf create-service p-mysql 10mb mysql

cf push appManager -p ..\build\libs\AppManager-0.0.1-SNAPSHOT.war -f appManager_manifest.yml

cf create-service-broker appManager admin admin https://appmanager.dev.cfdev.sh
#cf update-service-broker appManager admin admin https://appmanager.dev.cfdev.sh

## Create service instance of restarter plan
cf create-service "App Lifecycle Manager" "App Restarter" appRestarter1

# crate binding to existing test app
cf bind-service test-app appRestarter1 -c '{\"time\":\"2m\"}'

# cleanup
# cf unbind-service test-app appRestarter1
# cf delete-service appRestarter1
# cf delete-service-broker appManager