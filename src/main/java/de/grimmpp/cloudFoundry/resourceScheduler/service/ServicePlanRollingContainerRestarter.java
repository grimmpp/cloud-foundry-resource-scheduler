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
import java.util.Map;

@Slf4j
@Service
public class ServicePlanRollingContainerRestarter extends IServicePlanBasedOnAppBinding {

    public static final String PLAN_ID = "50afb061-866d-44ff-9af8-53e5547f330e";

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
        String time = TimeParameterValidator.getParameterFixedDelay(request, TimeParameterValidator.DEFAULT_VALUE);
        pRepo.save(
                Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(Parameter.KEY_FIXED_DELAY)
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

            // Are all application instances in state running? Is the app healthy?
            if (ais.values().stream().allMatch(i -> i.getState().equals("RUNNING"))) {
                log.debug("All app instances are in state RUNNING.");

                Map.Entry<String,AppInstanceState> oldestAi = ais.entrySet().stream().sorted(
                        (e1, e2) -> e2.getValue().getUptime().compareTo(e1.getValue().getUptime())).findFirst().get();
                boolean isExpired = oldestAi.getValue().getUptime() > timeInSec;
                int index = Integer.valueOf( oldestAi.getKey() );

                if (isExpired) {
                    String logLine = String.format("app instance %s, app: %s, space: %s, org: %s, si: %s, plan: %s",
                            index, b.getApplicationId(), si.getSpaceId(), si.getOrgId(), si.getServiceInstanceId(), PLAN_ID);

                    try {
                        String instanceUrl = cfClient.buildUrl(CfClient.URI_APP_INSTANCE, false, b.getApplicationId(), String.valueOf(index));
                        cfClient.deleteResource(instanceUrl);

                        log.info("=> Restarted " + logLine);
                    } catch (Throwable e) {
                        log.error("Cannot restart instance: " + index + ", " + logLine, e);
                    }
                } else {
                    log.debug("No app instance is expired of app {} {}.", app.getEntity().getName(), app.getMetadata().getGuid());
                }
            } else {
                log.debug("Cannot restart container because not all containers are in a health state.");
                for (Map.Entry<String,AppInstanceState> e: ais.entrySet()) {
                    log.debug("App {} {}, Instance: {}, State: {} ",
                            app.getEntity().getName(), app.getMetadata().getGuid(),
                            e.getKey(), e.getValue().getState());
                }
            }
        } else {
            log.debug("App {} {} is not started. ", app.getEntity().getName(), app.getMetadata().getGuid());
        }
    }
}
