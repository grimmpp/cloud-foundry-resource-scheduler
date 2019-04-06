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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ServicePlanSwitchOffWholeSpace extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "ea645636-3fd6-430e-90dc-4c8af5dce6a8";

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {

        String spaceUrl = cfClient.buildUrl(CfClient.URI_SINGLE_SPACE, si.getSpaceId());
        Resource<Space> space = cfClient.getResource(spaceUrl, Space.class);

        // Check if space contains prod in its name.
        if (!space.getEntity().getName().toLowerCase().contains("prod")) {

            if (isExpired(si)) {
                // Get apps
                String url = cfClient.buildUrl(CfClient.URI_APPS_OF_SPACE, si.getSpaceId());
                List<Resource<Application>> apps = cfClient.getResources(url, Application.class);
                log.debug("Found {} apps in space: {} for service instance {}", apps.size(), PLAN_ID, si.getServiceInstanceId());

                for (Resource<Application> app : apps) {

                    if (app.getEntity().getState().equals("STARTED")) {
                        String logLine = String.format("app: %s, space: %s because of serviceInstance: %s",
                                app.getMetadata().getGuid(), si.getSpaceId(), si.getServiceInstanceId());

                        try {
                            String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, app.getMetadata().getGuid());
                            cfClient.updateResource(appUrl, "{\"state\": \"STOPPED\"}", Application.class);

                            log.info("=> Stopped app " + logLine);
                        } catch (Throwable e) {
                            log.error("Cannot stop " + logLine, e);
                        }
                    } else {
                        log.debug("App {} was already stopped.", app.getMetadata().getGuid());
                    }
                }
                storeLastCall(si);
            } else {
                log.debug("Nothing to do for service instance {}", si.getServiceInstanceId());
            }
        } else {
            log.debug("Cancelled space {} consideration because it contains 'prod' in its name: {}",
                    space.getMetadata().getGuid(), space.getEntity().getName());
        }
    }

    private boolean isExpired(ServiceInstance si) throws IOException {
        List<Parameter> params = pRepo.findByReference(si.getServiceInstanceId());
        return TimeParameterValidator.isTimesExpired(params);
    }

    private void storeLastCall(ServiceInstance si) {
        // Remember last http call made.
        List<Parameter> params = pRepo.findByReference(si.getServiceInstanceId());
        Parameter p = Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL);
        if (p==null) {
            p = Parameter.builder()
                    .reference(si.getServiceInstanceId())
                    .key(Parameter.KEY_LAST_CALL)
                    .build();
        }
        p.setValue(Long.toString(System.currentTimeMillis()));
        pRepo.save(p);
        log.debug("Remembered last shutdown of space.");
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {
        if (!TimeParameterValidator.validateParameterValue(request.getParameters())) {
            throw new RuntimeException("Invalid time parameters.");
        }

        List<Parameter> params = new ArrayList<>();

        params.add(TimeParameterValidator.getTimeParameter(request));

        params.add(Parameter.builder()
                .reference(request.getServiceInstanceId())
                .key(Parameter.KEY_LAST_CALL)
                .value("0")
                .build());

        pRepo.saveAll(params);
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {
        // Nothing to do.
    }
}
