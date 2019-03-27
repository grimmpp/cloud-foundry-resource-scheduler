package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.model.cfClient.Application;
import de.grimmpp.AppManager.model.cfClient.ApplicationInstances;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.database.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class IServicePlanBasedOnAppBinding extends IServicePlanBasedOnServiceInstance {

    protected abstract void performActionForBinding(ServiceInstance si, Binding b, Resource<Application> app, Long time) throws IOException;

    @Override
    public void performActionForServiceInstance(ServiceInstance si) throws IOException {

        for(Binding b: bRepo.findByServiceInstanceId(si.getServiceInstanceId())) {
            log.debug("Check Binding: {}, Apps: {}", b.getBindingId(), b.getApplicationId());

            String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, false, b.getApplicationId());
            Resource<Application> app = cfClient.getResource(appUrl, Application.class);
            log.trace("App data: {}", objectMapper.writeValueAsString(app));

            Parameter p = pRepo.findByReferenceAndKey(b.getBindingId(), "time");
            long time = TimeParameterValidator.getTimeInMilliSecFromParameterValue(p.getValue());
            log.debug("Time: parameter value: {}, milli sec: {}", p.getValue(), time);

            performActionForBinding(si, b, app, time);
        }

    }
}
