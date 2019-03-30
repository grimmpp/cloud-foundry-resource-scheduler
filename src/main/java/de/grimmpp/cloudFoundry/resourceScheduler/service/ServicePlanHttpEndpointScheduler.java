package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class ServicePlanHttpEndpointScheduler extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "d0704f41-4a2e-4bea-b1f7-2319640cbe97";

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {

    }

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {

    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {

    }
}
