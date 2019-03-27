package de.grimmpp.AppManager.service;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import java.io.IOException;

public interface IServicePlan {
    String getServicePlanId();
    void saveRequestParamters(CreateServiceInstanceRequest request);
    void saveRequestParamters(CreateServiceInstanceBindingRequest request);

    void run() throws IOException;
}
