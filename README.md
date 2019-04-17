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
* **Partitioning of scheduler jobs**: The scheduler jobs will be divided by the amount of configured application instances. 
  Every application instance processes one part of all jobs. One part for an application instance is determined by the object ID in the database modulo 
  of the amount of application instances which then must match to the application index. 
  See details in <a href=".src/main/java/de/grimmpp/cloudFoundry/resourceScheduler/model/database/ServiceInstanceRepository.java">ServiceInstanceRepository.java</a>. 
* **Multi-Threaded processing of service plans**: Each service plan is processed one after the other. 
  This makes it easier to keep the memory consumption smaller and the logging output is also better to read. 
  For the processing of a service plan you can set the environment variable `max-threads-per-service-plan-scheduler` 
  in order to define the max parallel running threads. This setting applies to all service plans.
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
* **Http Sender Identifier**: In every call which is done by the Resource Scheduler application there is a header 
  `X-CF-SENDER-APP-INSTANCE` set, containing the application GUID and the application index 
  so that every called component can identify from where the request was sent. 
* **Optimized Memory Settings:** The memory consumption was analysed. Mainly it depends on how many concurrent 
  worker threads are running in parallel and how many service instances are created. In general the 
  <a href="https://github.com/cloudfoundry/java-buildpack-memory-calculator">Memory Calculator</a> 
  which is bundled by default in the <a href="https://github.com/cloudfoundry/java-buildpack">java-buildpack</a>
  does a very good job. The only thing which came out during the tests was that ReservedCodeCacheSize is always 
  set to 250MB which is for this application quite high. By setting `JAVA_OPTS: '-XX:ReservedCodeCacheSize=50M'`  I 
  could reduce the amount of Memory by 200MB. See also for example settings: 
  <a href="./deployIntoCfdev/resourceScheduler_manifest.yml">resourceScheduler_manifest.yml</a>
* **Planned Things**
  * sync job which checks if broker db is in same state like cf db, only regarding its own instances.
  * Tracing for http communication
  * Improve detection of what should be triggered so that there are not that many calls against cloud foundry or its own database.
  * Collect list of failed calls and make it available for service plan instance owner.
  * Create statistics incl. memory consumption and processing durations of service plans. 

## How to build and run unit tests
````
./gradlew clean build
````

## Prerequisites for depolying 
For deploying this application you require **Cloud Foundry** and a **relational database**.

## What to configure
Check out application.yml there you can find the necessary environment variable to be set:
* BROKER_BASIC_AUTH_USERNAME
* BROKER_BASIC_AUTH_PASSWORD
* CF_ADMIN_USERNAME
* CF_ADMIN_PASSWORD

You may need to set a specific hibernate dialect like: 
* spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.MariaDBDialect

## How to deploy
For cfdev see: <a href="./deployIntoCfdev/deploy.bat">./deployIntoCfdev/deploy.bat</a>

## How to use only NONE Cloud Foundry Service Plans
If you don't want to use the Cloud Foundry API, you can run the Resource Scheduler also without Cloud Foundry user.
In any case you need to register the Resource Scheduler as a service broker to Cloud Foundry so that you are able to do 
the configuration of the jobs. Jobs will be configured by creating service instances of service plans or by creating 
bindings to applications. ... For details have a look into the 
<a href="./src/main/java/de/grimmpp/cloudFoundry/resourceScheduler/config/CatalogConfig.java">service catalog</a>. 
<br />What you need to configure if you don't want to use Cloud Foundry API at all is the following:
````
application-instances-count: 1  # When every you scale up or down the application you need to adjust this value.
````
**Advice**: You can create a Cloud Foundry user within the same space of the Resource Scheduler and assign the role 
'SpaceAuditor' (readonly for that single space) to it so that the Resource Scheduler can query for the amount of 
configured instances. This allows you to use all service plans which have no need to interact with Cloud Foundry and 
you can still benefit from the load distribution of the Resource Scheduler.

**Warning**: If the amount of configured application instances is not in sync. jobs will be triggered less or more than defined.  

## Links
* Service Broker Api Framework: https://spring.io/projects/spring-cloud-open-service-broker
* cfdev for installing Cloud Foundry locally: https://github.com/cloudfoundry-incubator/cfdev
* cfdev Cloud Foundry CLI Plugin: https://plugins.cloudfoundry.org/#cfdev 
