package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ServicePlanFinder;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.BindingRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class IServicePlanBasedOnServiceInstance implements IServicePlan {

    @Autowired
    protected CfClient cfClient;

    @Autowired
    protected ServiceInstanceRepository siRepo;

    @Autowired
    protected BindingRepository bRepo;

    @Autowired
    protected ParameterRepository pRepo;

    @Autowired
    protected AppConfig appConfig;

    protected ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    protected abstract void performActionForServiceInstance(ServiceInstance si) throws IOException;

    @PostConstruct
    public void init() {
        ServicePlanFinder.registerServicePlan(getServicePlanId(), this);
        if (appConfig.isSchedulingEnabled()) log.debug("Service Plan {} is activated.", getClass().getSimpleName());
        else log.debug("Service Plan {} is NOT activated.", getClass().getSimpleName());
    }

    @Override
    public void run() throws IOException {
        String planId = getServicePlanId();
        log.debug("Start run of {} and for plan id {}", getClass().getSimpleName(), planId);

        long startTime = System.currentTimeMillis();

        List<ServiceInstance> serviceInstances = siRepo.findByServicePlanIdAndAppInstanceIndex(planId, appConfig.getCfInstanceIndex(), appConfig.getAmountOfInstances());
        log.debug("{} service instances to be processed. ", serviceInstances.size());

        for(ServiceInstance si: serviceInstances) {
            log.debug("Check service instance: {}, plan: {}, org: {}, space: {}",
                    si.getServiceInstanceId(),
                    si.getServicePlanId(),
                    si.getOrgId(),
                    si.getSpaceId());

            this.performActionForServiceInstance(si);
        }

        long d = System.currentTimeMillis() - startTime;
        long dMilli = d % 1000;
        long dSec = (d / 1000) % 60;
        long dMin = d / (1000 * 60);
        log.debug("Handled {} registered service instances for service plan: {} in duration of {}min {}sec {}milli for application instance {}",
                serviceInstances.size(), getClass().getSimpleName(), dMin, dSec, dMilli, appConfig.getVcapApplication().getInstance_index());
    }
}
