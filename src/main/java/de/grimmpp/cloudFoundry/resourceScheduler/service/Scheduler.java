package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ServicePlanFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class Scheduler {

    @Autowired
    private CfClient cfClient;

    @Autowired
    private AppConfig appConfig;


    @Scheduled(fixedDelay = 30 * 1000) // 30sec
    public void scheduledRun() throws IOException {
        log.debug("Scheduler was triggered.");
        if (appConfig.isSchedulingEnabled()) {

            appConfig.updateAmountOfInstances( cfClient.getRunningInstanceOfResourceSchedulerApp() );

            for(IServicePlan plan: ServicePlanFinder.getServicePlans()) {
                try {
                    plan.run();
                } catch (Throwable e) {
                    log.error("Problem during service plan run.", e);
                }
            }
        }
    }
}
