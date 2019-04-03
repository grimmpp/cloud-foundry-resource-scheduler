package de.grimmpp.cloudFoundry.resourceScheduler.config;

import de.grimmpp.cloudFoundry.resourceScheduler.service.ServicePlanAppRestarter;
import de.grimmpp.cloudFoundry.resourceScheduler.service.ServicePlanSwitchOffAppsInSpace;
import org.springframework.cloud.servicebroker.model.catalog.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {

    @Bean
    public Catalog getCatalog() {
        return Catalog.builder()
                .serviceDefinitions(
                    ServiceDefinition.builder()
                        .id("385fcdd0-f9f1-42c9-929a-93981441b0b1")
                        .name("Resource Scheduler")
                        .description("Provides several service plans in order to schedule different tasks or to trigger REST APIs.")
                        .metadata("displayName", "Resource Scheduler")
                        .metadata("imageUrl", "")
                        .metadata("longDescription", "")
                        .metadata("documentationUrl", "https://github.com/grimmpp/cloud-foundry-resource-scheduler")
                        .metadata("supportUrl","https://github.com/grimmpp/cloud-foundry-resource-scheduler")
                        .plans(

                            Plan.builder()
                                    .id("d0704f41-4a2e-4bea-b1f7-2319640cbe97")
                                    .name("HttpEndpointScheduler")
                                    .description("Triggers defined http endpoint after a defined time. After the scheduler triggered all endpoints it will pause for 30 sec.")
                                    .metadata("bullets", new String[]{
                                        "Client timeout for the scheduler is set to 500 milli sec. The scheduler is NOT evaluating and is NOT waiting for a response.",
                                        "Either 'fixedDelay' or 'times' must be chosen as parameter. Both parameters are exclusive.",
                                        "Exclusive Parameter \"{\"fixedDelay\": \"1w 3d 2h 5min\"}\" (w=week, d=day, h=hour, m=minute) is a time period of time after which the trigger starts.",
                                        "Exclusive Parameter \"{\"times\": [\"09:05\", \"12:05\", \"15:05\", \"20:05\"] is a list of points in time [hour:min]. When one of those times are reached then the trigger starts.",
                                        "Mandatory git Parameter \"{\"url\": \"https://full-url.com\"}\" is the url which will be triggered.",
                                        "Optional Parameter \"{\"httpMethod\": \"GET\"}\" (values: GET, PUT, POST, DELETE) is the http method which will be set. (Default value is GET)",
                                        "Optional Parameter \"{\"httpHeaders\": [\"Content-Type: application/json; charset=UTF-8\"]} ",
                                        "Optional Parameter \"{\"sslEnabled\": false} (Default value is true) "
                                    })
                                    .bindable(false)
                                    .free(true)
                                    .build(),

                            Plan.builder()
                                    .id(ServicePlanAppRestarter.PLAN_ID)
                                    .name("AppRestarter")
                                    .description("Bound apps to service instances of AppRestarter will be restarted after a defined time. (Default time is 8h) All app instances will be restarted at the same point in time.")
                                    .bindable(true)
                                    .free(true)
                                    .build(),
/*
                            Plan.builder()
                                    .id("e3e8719a-2994-49f5-ac6a-e3fffc3673a4")
                                    .name("Rolling Container Restarter")
                                    .description("Restarts apps which are bind to this service plan in a rolling manner. It restarts single container, one after another, and only if all others in a healthy state.")
                                    .bindable(true)
                                    .free(true)
                                    .build(),
//*/
                            Plan.builder()
                                    .id(ServicePlanSwitchOffAppsInSpace.PLAN_ID)
                                    .name("SwitchOffAppsInSpace")
                                    .description("Stops all apps after a defined time in the space where this service plan is instanced, except in spaces which contain prod in the name in order to avoid downtime of apps in productive spaces. ")
                                    .bindable(false)
                                    .free(true)
                                    .build()
/*
                                , Plan.builder()
                                    .id("0ba66ba0-e6a0-44d4-af50-059ac98a69cd")
                                    .name("Switch App Off")
                                    .description("")
                                    .bindable(true)
                                    .free(true)
                                    .build()
//*/
                            )
                    .build()
                )
                .build();
    }

}

