package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.cfClient.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ServicePlanSwitchOffAppsInSpace {

    public static final String PLAN_ID = "4e1020f9-6577-4ba3-885f-95bb978b4939";

    @Autowired
    CfClient cfClient;

    public void run() throws IOException {
        Set<String> spaceIds = new HashSet<>();
        // Get all service instances for this plan and iterate through ..
        List<Resource<ServiceInstance>> serviceInstances = cfClient.getServiceInstances(PLAN_ID);
        for(Resource<ServiceInstance> si: serviceInstances) {

            spaceIds.add( si.getEntity().getSpace_guid() );
        }

        for(String sId: spaceIds) {
            String url = cfClient.buildUrl(CfClient.URI_APPS_OF_SPACE, sId);
            List<Resource<Application>> apps = cfClient.getResources(url, Application.class);

            for(Resource<Application> app: apps) {
                String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, app.getMetadata().getGuid());
                cfClient.updateResource(appUrl, "{\"state\": \"STOPPED\"}", String.class);
            }
        }
    }
}
