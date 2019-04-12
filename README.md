# Cloud Foundry Resource Scheduler Service
The purpose of Resource Scheduler is to trigger endpoints after a defined timespan. There are some specific use cases implemented as service plans like restarting of applications or switching them off after a given time.
There is also a generic service plan available which allows to trigger arbitrary http endpoint. In the service <a href="./src/main/java/de/grimmpp/cloudFoundry/resourceScheduler/config/CatalogConfig.java">catalog</a> you can find the list of supported service plans. 

The Resource Scheduler consists of two parts:
1. Service Broker which can be registered in the Cloud Foundry marketplace and all service can be regularly booked via Cloud Foundry commands.
2. Scheduler which triggers the defined endpoints and time periodes. What kind of endpoint will be triggered is defined in each service plan.

Both parts, Service Broker and Scheduler, is contained within the same application. You only need to deploy one applicatin into Cloud Foundry.

## Service Plan Overview
For details have a look into the <a href="./src/main/java/de/grimmpp/cloudFoundry/resourceScheduler/config/CatalogConfig.java">catalog</a> file.
* **AppRestarter**: Restarts frequently a bound app after a defined time completely. (All instances at the same time) 
* **RollingContainerRestarter**: Restart of an application without downtime. Only single containers will be restarted. This service plan can be used for simulating Cloud Foundry updates or for something like a friendly Chaos Monkey. 
* **SwitchOffAppsInSpace**: Switches off all apps after a defined time in a space which doesn't contain 'prod' in the name. (No bindings needed) This plan can be used for test and demo spaces in order to save resources from the quota.
* **SwitchOffWholeSpace**: Switches off all apps in a space at defined times. It will also ignore spaces containing 'prod' in their names. (No bindings needed) This plan can be used for test and demo spaces in order to save resources from the quota.
* **HttpEndpointScheduler**: Triggers frequently arbitrary http endpoints after a defined periode of time or for specific points in time e.g. 5pm.


## Technical data
* Used Spring Cloud Open **Service Broker API**
* The application is a **multi-instance application**. It can be scaled to more than one application instance.
* **Load Distribution**: If the app runs in more than one instance then the instances will split up the service instances to be processed. For that an application instance needs to know how many instances are configured in total. It makes frequently calls against the Cloud Foundry API to find that out. 
* **Spring Boot** is used as Java Framework
  * **Spring Boot Security** (Basic Auth) is implemented.
  * **Spring Boot JPA & Hibernate** is used for DB connection.
  * **Cloud Foundry API Client** is self-developed and contained within this project. (This was done because I wanted to be able to run all locally based on the junit test data.)
    * For every call an identification header which contains the app guid and container index of this scheduler application is sent so that it is visible for the receiver app which app was the sender.
    * SSL can be disabled for testing for all calls. See property: `cfClient.SSL-Validation-enabled`
    * For specific instances SSL can also be disabled for that please see the service plan description.
* In the section/module test there is an additional RestController which **mocks** the **Cloud Foundry API** in order to test the full roundtrip of API calls to Cloud Foundry. (OAuth tests are not included.)
* **Lombock** is used to keed class definitions simpler.
* Catalog descriptions can be trimmed optionally because of PCF DB field length limitations (255 chars) `trim-catalog-descriptions: true`
* **Planned Things**
  * Multi-threaded scheduler for all service plans
  * sync job which checks if broker db is in same state like cf db, only regarding its own instances.
  * reduction of memory consumption
  * Tracing for http communication
  * Improve detection of what should be triggered so that there are not that many calls against cloud foundry or its own database.
  * Collect list of failed calls and make it available for service plan instance owner.

## How to build and run unit tests
````
./gradlew clean build
````

## Prerequisites for depolying 
For deploying this application you require a **Cloud Foundry User** and a **relational database**. <br />
**Deployment Options:**
1. You can deploy the Resource Scheduler and make it globally available for all organizations in Cloud Foundry. For this scenario you will require a Cloud Foundry Admin account which has got admin rights in order to e.g. restart apps. The actions which will be performed are dependent on the service instances which are provisioned and how they are configured.
2. You can deploy the Resource Scheduler and register it as <a href="https://docs.cloudfoundry.org/services/managing-service-brokers.html#register-broker">space scoped service broker</a>. For this scenario you only require a Cloud Foundry User which has got SpaceDeveloper role in your spaces where you want to use the Resource Scheduler service plan instances.
3. If you only want to use the HttpEndpointScheduler then their is no need to configure a Cloud Foundry User at all. The only this you need to provide is the amount of configured application instances of the Resource Scheduler so that it doesn't need to ask the Cloud Foundry API for it. The environment variable is defined like e.g. `application-instances-count=4` (Everytime you scale Resource Scheduler you will need to adapt the environment variable.)

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
