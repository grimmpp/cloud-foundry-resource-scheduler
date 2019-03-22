package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.Parameter;
import de.grimmpp.AppManager.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ServicePlanSwitchOffAppsInSpace extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "4e1020f9-6577-4ba3-885f-95bb978b4939";

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {

        String url = cfClient.buildUrl(CfClient.URI_APPS_OF_SPACE, si.getSpaceId());
        List<Resource<Application>> apps = cfClient.getResources(url, Application.class);
        log.debug("Collected {} apps from space: {}", apps.size(), PLAN_ID);

        for (Resource<Application> app : apps) {

            long currentTime = System.currentTimeMillis();
            if (app.getEntity().getState().equals("STARTED")) {
                String logLine = String.format("app: %s, space: %s because of serviceInstance: %s",
                        app.getMetadata().getGuid(), si.getSpaceId(), si.getServiceInstanceId());

                Parameter p = pRepo.findByReferenceAndKey(si.getServiceInstanceId(), TimeParameterValidator.KEY);
                long time = TimeParameterValidator.getTimeInMilliSecFromParameterValue(p.getValue());

                long timeDiff = currentTime - app.getMetadata().getUpdated_at().getTime();
                if (timeDiff > time) {
                    try {
                        String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, app.getMetadata().getGuid());
                        cfClient.updateResource(appUrl, "{\"state\": \"STOPPED\"}", String.class);

                        log.info("Stopped ");
                    } catch (Throwable e) {
                        log.error("Cannot stop " + logLine, e);
                    }
                }
            }
        }
    }

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }
}
