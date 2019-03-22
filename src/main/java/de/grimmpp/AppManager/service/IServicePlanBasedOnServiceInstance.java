package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.ApplicationInstances;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public abstract class IServicePlanBasedOnServiceInstance implements IServicePlan{

    protected abstract CfClient cfClient();
    protected abstract ServiceInstanceRepository siRepo();
    protected abstract void performAction(String instanceId, String spaceId, String orgId);


    public void run() throws IOException {
        String planId = getServicePlanId();

        for(ServiceInstance si: siRepo().findByServicePlanId(planId)) {
            performAction(si.getServiceInstanceId(), si.getSpaceId(), si.getOrgId());
        }
    }
}
