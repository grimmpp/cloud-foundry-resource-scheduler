package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.ApplicationInstances;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.*;

import java.io.IOException;

public abstract class IServicePlanBasedOnAppBinding extends IServicePlanBasedOnServiceInstance {

    protected abstract BindingRepository bRepo();
    protected abstract ParameterRepository pRepo();
    protected abstract void performAction(String instanceId, String spaceId, String orgId);

    @Override
    public void run() throws IOException {
        String planId = getServicePlanId();

        for(ServiceInstance si: siRepo().findByServicePlanId(planId)) {
            for(Binding b: bRepo().findByServiceInstanceId(si.getServiceInstanceId())) {
                String appUrl = cfClient().buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
                Resource<Application> app = cfClient().getResource(appUrl, Application.class);

                Parameter p = pRepo().findByReferenceAndKey(b.getServiceInstanceId(), "time");
                long time = 0; //TODO: to be set with value from parameter

                if (app.getEntity().getState().equals("STARTED")) {

                    String aiUrl = cfClient().buildUrl(CfClient.URI_APP_INSTANCES, b.getApplicationId());
                    ApplicationInstances ais = cfClient().getObject(aiUrl, ApplicationInstances.class);

                    boolean isAppExpired = ais.values().stream().anyMatch(i -> i.getUptime().equals(time));

                    if (isAppExpired) {
                        for (int i = 0; i < app.getEntity().getInstances(); i++) {
                            String instanceUrl = cfClient().buildUrl(CfClient.URI_APP_INSTANCE, b.getApplicationId(), String.valueOf(i));
                            cfClient().deleteResource(instanceUrl);
                        }
                    }
                }
            }
        }

        for(ServiceInstance si: siRepo().findByServicePlanId(planId)) {


            performAction(si.getServiceInstanceId(), si.getSpaceId(), si.getOrgId());
        }
    }
}
