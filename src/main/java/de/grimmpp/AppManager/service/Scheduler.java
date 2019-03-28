package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.helper.ServicePlanFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class Scheduler {

    @Value("${scheduling-enabled}")
    private Boolean schedulingEnabled;

    @Scheduled(fixedDelay = 30 * 1000) // 30sec
    public void scheduledRun() throws IOException {
        log.debug("Scheduler was triggered.");
        if (schedulingEnabled) {
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
