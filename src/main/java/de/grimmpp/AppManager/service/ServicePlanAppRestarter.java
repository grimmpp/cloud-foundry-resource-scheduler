package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.ApplicationInstances;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class ServicePlanAppRestarter extends IServicePlanBasedOnAppBinding {

    public static final String PLAN_ID = "31b97c09-9cfb-4108-8894-33eb22016cee";

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {
        // Nothing to do.
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {
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
    protected void performActionForBinding(ServiceInstance si, Binding b, Resource<Application> app, Long time) throws IOException {

        if (app.getEntity().getState().equals("STARTED")) {
            log.debug("App {} is in state: STARTED", b.getApplicationId());

            String aiUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCES, false, b.getApplicationId());
            ApplicationInstances ais = cfClient.getObject(aiUrl, ApplicationInstances.class);
            log.trace("App Instances: {}", objectMapper.writeValueAsString(ais));

            long expiredAIs = ais.values().stream().filter(i -> Long.valueOf(i.getUptime()) < time).count();
            log.debug("Expired app instances: {}", expiredAIs);

            if (expiredAIs > 0) {
                for (int i = 0; i < app.getEntity().getInstances(); i++) {

                    String logLine = String.format("app instances %s, app: %s, space: %s, org: %s, si: %s, plan: %s",
                            i, b.getApplicationId(), si.getSpaceId(), si.getOrgId(), si.getServiceInstanceId(), PLAN_ID);

                    try {
                        String instanceUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCE, false, b.getApplicationId(), String.valueOf(i));
                        cfClient.deleteResource(instanceUrl);

                        log.info("Restarted "+logLine);
                    } catch (Throwable e) {
                        log.error("Cannot restart instance: "+i+", "+logLine, e);
                    }
                }
            }
        }
    }
}
