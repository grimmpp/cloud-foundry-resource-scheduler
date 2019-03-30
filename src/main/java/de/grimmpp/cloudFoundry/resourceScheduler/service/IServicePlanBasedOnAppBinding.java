package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Application;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Resource;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Binding;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
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
            long timeInSec = time / 1000;
            log.debug("Time: parameter value: {}, milli sec: {}", p.getValue(), timeInSec);

            performActionForBinding(si, b, app, timeInSec);
        }

    }
}
