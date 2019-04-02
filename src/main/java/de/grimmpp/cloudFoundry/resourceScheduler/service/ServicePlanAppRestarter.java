package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.AppInstanceState;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Application;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.ApplicationInstances;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Resource;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Binding;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
        // requires parameter "fixedDelay"
        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        pRepo.save(
                Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(TimeParameterValidator.KEY_FIXED_DELAY)
                        .value(time)
                        .build());
    }


    @Override
    protected void performActionForBinding(ServiceInstance si, Binding b, Resource<Application> app, Long timeInSec) throws IOException {

        if (app.getEntity().getState().equals("STARTED")) {
            log.debug("App {} is in state: STARTED", b.getApplicationId());

            String aiUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCES, false, b.getApplicationId());
            ApplicationInstances ais = cfClient.getObject(aiUrl, ApplicationInstances.class);
            log.trace("App Instances: {}", objectMapper.writeValueAsString(ais));

            List<AppInstanceState> expiredAis = ais.values().stream().filter(i -> i.getUptime() > timeInSec).collect(Collectors.toList());
            log.debug("Expired app instances: {}", expiredAis.size());
            for(String index : ais.keySet()) {
                AppInstanceState state = ais.get(index);
                log.debug("Compare time: App {}, instance {}, state: {}, uptime: {} sec", app.getMetadata().getGuid(), index, state.getState(), state.getUptime());
            }

            if (expiredAis.size() > 0) {
                for (int i = 0; i < app.getEntity().getInstances(); i++) {

                    String logLine = String.format("app instances %s, app: %s, space: %s, org: %s, si: %s, plan: %s",
                            i, b.getApplicationId(), si.getSpaceId(), si.getOrgId(), si.getServiceInstanceId(), PLAN_ID);

                    try {
                        String instanceUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCE, false, b.getApplicationId(), String.valueOf(i));
                        cfClient.deleteResource(instanceUrl);

                        log.info("=> Restarted "+logLine);
                    } catch (Throwable e) {
                        log.error("Cannot restart instance: "+i+", "+logLine, e);
                    }
                }
            }
        }
    }
}
