package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.Binding;
import de.grimmpp.AppManager.model.database.ServiceInstance;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class ServicePlanTestBasedOnBinding extends IServicePlanBasedOnAppBinding {
    public String planId = UUID.randomUUID().toString();
    public int runCount = 0;

    @Override
    protected void performActionForBinding(ServiceInstance si, Binding b, Resource<Application> app, Long time) throws IOException {
        try {
            Thread.sleep(1234);
            runCount++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getServicePlanId() {
        return planId;
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {

    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {

    }
}
