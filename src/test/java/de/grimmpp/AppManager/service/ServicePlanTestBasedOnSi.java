package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class ServicePlanTestBasedOnSi extends IServicePlanBasedOnServiceInstance {
    public String planId = UUID.randomUUID().toString();
    public int runCount = 0;

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {
        try {
            Thread.sleep(1234);
            runCount++;
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }
    @Override
    public String getServicePlanId() { return planId; }
}
