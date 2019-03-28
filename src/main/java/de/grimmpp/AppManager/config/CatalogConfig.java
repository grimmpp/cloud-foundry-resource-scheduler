package de.grimmpp.AppManager.config;

import de.grimmpp.AppManager.service.ServicePlanAppRestarter;
import de.grimmpp.AppManager.service.ServicePlanSwitchOffAppsInSpace;
import org.springframework.cloud.servicebroker.model.catalog.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class CatalogConfig {

    @Bean
    public Catalog getCatalog() {
        return Catalog.builder()
                .serviceDefinitions(
                    ServiceDefinition.builder()
                        .id("385fcdd0-f9f1-42c9-929a-93981441b0b1")
                        .name("App Lifecycle Manager")
                        .description("Provides several service plans in order to restart or switch off apps.")
                        .metadata("displayName", "App Manager Service")
                        .metadata("imageUrl", "")
                        .metadata("longDescription", "")
                        .metadata("documentationUrl", "")
                        .metadata("supportUrl","")
                        .plans(
                            Plan.builder()
                                    .id(ServicePlanAppRestarter.PLAN_ID)
                                    .name("App Restarter")
                                    .description("Bound apps to service instances of the plan App Restarter will be restarted by the specified and given time. (Default time is 8h) All app instances will be restarted at the same point in time.")
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
                                    .name("Switch Off Apps in Space")
                                    .description("Stops all apps after a defined time and in a space where this service plan is instanced, except in spaces which contain prod in the name in order to avoid downtime of apps in productive spaces. ")
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

