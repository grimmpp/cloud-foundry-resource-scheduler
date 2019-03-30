package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Application;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Resource;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Space;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ServicePlanSwitchOffAppsInSpace extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "4e1020f9-6577-4ba3-885f-95bb978b4939";

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {

        String spaceUrl = cfClient.buildUrl(CfClient.URI_SINGLE_SPACE, si.getSpaceId());
        Resource<Space> space = cfClient.getResource(spaceUrl, Space.class);

        // Check if space contains prod in its name.
        if (!space.getEntity().getName().toLowerCase().contains("prod")) {

            String url = cfClient.buildUrl(CfClient.URI_APPS_OF_SPACE, si.getSpaceId());
            List<Resource<Application>> apps = cfClient.getResources(url, Application.class);
            log.debug("Found {} apps in space: {} for service instance {}", apps.size(), PLAN_ID, si.getServiceInstanceId());

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
                            cfClient.updateResource(appUrl, "{\"state\": \"STOPPED\"}", Application.class);

                            log.info("=> Stopped app " + logLine);
                        } catch (Throwable e) {
                            log.error("Cannot stop " + logLine, e);
                        }
                    } else {
                        log.debug("App {} is not expired. Last app update: {}", app.getMetadata().getGuid(), app.getMetadata().getUpdated_at());
                    }
                } else {
                    log.debug("App {} is in state {}", app.getMetadata().getGuid(), app.getEntity().getState());
                }
            }
        } else {
            log.debug("Cancelled space {} consideration because it contains 'prod' in its name: {}",
                    space.getMetadata().getGuid(), space.getEntity().getName());
        }
    }

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {
        // requires parameter "time"
        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        pRepo.save(
                Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(TimeParameterValidator.KEY)
                        .value(time)
                        .build());
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {
        // Nothing to do.
    }
}
