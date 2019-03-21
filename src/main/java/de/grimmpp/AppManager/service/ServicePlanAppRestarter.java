package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.ApplicationInstances;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.*;
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

    @Autowired
    private ParameterRepository parameterRepo;

    @Override
    public void run() throws IOException {

        for(ServiceInstance si: siRepo.findByServicePlanId(PLAN_ID)) {
            for(Binding b: bindingRepo.findByServiceInstanceId(si.getServiceInstanceId())) {
                String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
                Resource<Application> app = cfClient.getResource(appUrl, Application.class);

                if (app.getEntity().getState().equals("STARTED")) {
                    Parameter p = parameterRepo.findByReferenceAndKey(b.getServiceInstanceId(), "time");
                    long time = 0; //TODO: to be set with value from parameter

                    String aiUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCES, b.getApplicationId());
                    ApplicationInstances ais = cfClient.getObject(aiUrl, ApplicationInstances.class);

                    boolean isAppExpired = ais.values().stream().anyMatch(i -> i.getUptime().equals(time));

                    if (isAppExpired) {
                        for (int i = 0; i < app.getEntity().getInstances(); i++) {
                            String instanceUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCE, b.getApplicationId(), String.valueOf(i));
                            cfClient.deleteResource(instanceUrl);
                        }
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
