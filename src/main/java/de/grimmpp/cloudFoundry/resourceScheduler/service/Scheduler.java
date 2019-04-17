package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ServicePlanFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class Scheduler {

    @Autowired
    private CfClient cfClient;

    @Autowired
    private AppConfig appConfig;

    @Scheduled(fixedDelay = 30 * 1000) // every 30 sec
    public void scheduledRun() throws IOException, InterruptedException {
        log.debug("Scheduler was triggered.");
        if (appConfig.isSchedulingEnabled()) {
            long startTime = System.currentTimeMillis();

            appConfig.updateAmountOfInstances( cfClient.getRunningInstanceOfResourceSchedulerApp() );

            for(final IServicePlan plan: ServicePlanFinder.getServicePlans()) {
                try {
                    plan.run();
                } catch (Throwable e) {
                    log.error("Problem during service plan run.", e);
                }
            }

            showTime(startTime, appConfig.getCfInstanceIndex());
        }
    }

    private void showTime(long startTimestamp, int appIndex) {
        long d = System.currentTimeMillis() - startTimestamp;
        long dMilli = d % 1000;
        long dSec = (d / 1000) % 60;
        long dMin = d / (1000 * 60);

        log.info("Processed all service plans in duration of {}min {}sec {}milli for application index/instance {}",
                dMin, dSec, dMilli, appIndex);
    }

    @Scheduled(initialDelay = 10*1000,   // initial delay of 10 sec
            fixedDelay = 10 * 60 * 1000) // every 10 min
    public void schduledGC() {
        if (appConfig.isSchedulingEnabled()) {

            // Detailed output can be enabled by: -XX:+PrintGCDetails
            log.debug("Trigger Garbage Collection: ");
            System.gc();
        }
    }
}
