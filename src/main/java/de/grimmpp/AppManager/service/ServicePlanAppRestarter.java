package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.Binding;
import de.grimmpp.AppManager.model.database.BindingRepository;
import de.grimmpp.AppManager.model.database.ServiceInstance;
import de.grimmpp.AppManager.model.database.ServiceInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ServicePlanAppRestarter implements IServicePlan {

    public static final String PLAN_ID = "31b97c09-9cfb-4108-8894-33eb22016cee";

    @Autowired
    CfClient cfClient;

    @Autowired
    private ServiceInstanceRepository siRepo;

    @Autowired
    private BindingRepository bindingRepo;

    @Override
    public void run() throws IOException {

        for(ServiceInstance si: siRepo.findByServicePlanId(PLAN_ID)) {
            for(Binding b: bindingRepo.findByServiceInstanceId(si.getServiceInstanceId())) {
                String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
                Resource<Application> app = cfClient.getResource(appUrl, Application.class);

                if (app.getEntity().getState().equals("STARTED")) {
                    for(int i=0; i<app.getEntity().getInstances();i++) {
                        String instanceUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCE, b.getApplicationId(), String.valueOf(i));
                        cfClient.deleteResource(instanceUrl);
                    }
                }
            }
        }

        //TODO: needs to be tested!
    }

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }
}
