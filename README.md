# WORK IN PROGRESS

# Cloud Foundry Resource Scheduler Service
The purpose of Resource Scheduler is to trigger endpoints after a given time. There are some specific use cases implemented as service plans like restarting applications or switching them off after a given time.
The is also a generic service plan available which allows to trigger arbitrary http endpoint. In the service <a href="./src/main/java/de/grimmpp/AppManager/config/CatalogConfig.java">catalog</a> you can find the list of supported service plans. 

The Resource Scheduler consists of two parts:
1. Service Broker which can be registered in the Cloud Foundry marketplace and all service can be regularly booked via Cloud Foundry commands.
2. Scheduler which triggers the defined endpoints. What is be triggered is defined in each service plan.

Both parts, Service Broker and Scheduler, is contained within the same application. 

## How to build and run unit tests
````
./gradlew clean build
````

## What to configure
Check out application.yml there you can find the necessary environment variable to be set:
* BROKER_BASIC_AUTH_USERNAME
* BROKER_BASIC_AUTH_PASSWORD
* CF_ADMIN_USERNAME
* CF_ADMIN_PASSWORD

You may need to set hibernate dialects like: 
* spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.MariaDBDialect

## How to deploy
For cfdev see: <a href="./deployIntoCfdev/deploy.bat">./deployIntoCfdev/deploy.bat</a>

## Links
* Service Broker Api Framework: https://spring.io/projects/spring-cloud-open-service-broker
* cfdev for installing Cloud Foundry locally: https://github.com/cloudfoundry-incubator/cfdev
* cfdev Cloud Foundry CLI Plugin: https://plugins.cloudfoundry.org/#cfdev 
