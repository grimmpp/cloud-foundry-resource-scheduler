package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.helper.ObjectMapperFactory;
import de.grimmpp.AppManager.helper.ServicePlanFinder;
import de.grimmpp.AppManager.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Modifier;

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

    @Value("${scheduling-enabled}")
    private Boolean schedulingEnabled;

    protected ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    protected abstract void performActionForServiceInstance(ServiceInstance si) throws IOException;

    @PostConstruct
    public void init() {
        ServicePlanFinder.registerServicePlan(getServicePlanId(), this);
        if (schedulingEnabled) log.debug("Service Plan {} is activated.", getClass().getSimpleName());
        else log.debug("Service Plan {} is NOT activated.", getClass().getSimpleName());
    }
/*
    @Scheduled(fixedDelay = 60 * 1000) // 1 min
    public void scheduledRun() throws IOException {
        log.debug("Scheduler triggered: {}", getClass().getSimpleName());
        if (schedulingEnabled) run();
    }*/

    @Override
    public void run() throws IOException {
        String planId = getServicePlanId();
        log.debug("Start run of {} and for plan id {}", getClass().getSimpleName(), planId);

        long startTime = System.currentTimeMillis();

        for(ServiceInstance si: siRepo.findByServicePlanId(planId)) {
            log.debug("Check service instance: {}, plan: {}, org: {}, space: {}",
                    si.getServiceInstanceId(),
                    si.getServicePlanId(),
                    si.getOrgId(),
                    si.getSpaceId());

            this.performActionForServiceInstance(si);
        }

        long d = System.currentTimeMillis() - startTime;
        long dMilli = d % 1000;
        long dSec = (d / 1000) % 60 ;
        long dMin = d / (1000 * 60);
        log.debug("Duration of run {}min {}sec {}milli", dMin, dSec, dMilli);
    }
}
