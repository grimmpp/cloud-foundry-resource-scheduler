package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.Binding;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.cfClient.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ServicePlanAppRestarter {

    public static final String PLAN_ID = "31b97c09-9cfb-4108-8894-33eb22016cee";

    @Autowired
    CfClient cfClient;

    public void run() throws IOException {
        // Get all service instances for this plan and iterate through ..
        List<Resource<ServiceInstance>> serviceInstances = cfClient.getServiceInstances(PLAN_ID);
        for(Resource<ServiceInstance> si: serviceInstances) {

            // Get all bindings for one service instances and iterate through ..
            String bindingUrl = cfClient.buildUrl(si.getEntity().getService_bindings_url());
            List<Resource<Binding>> bindings = cfClient.getResources(bindingUrl, Binding.class);
            for(Resource<Binding> b: bindings) {
                String appUrl = cfClient.buildUrl(b.getEntity().getApp_url());
                Resource<Application> app = cfClient.getResource(appUrl, Application.class);

                //TODO: continue
            }
        }
    }
}
