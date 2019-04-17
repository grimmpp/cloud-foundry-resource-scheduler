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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

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

    private ThreadPoolExecutor threadExecutor;

    protected abstract void performActionForServiceInstance(ServiceInstance si) throws IOException;

    @PostConstruct
    public void init() {
        ServicePlanFinder.registerServicePlan(getServicePlanId(), this);
        if (appConfig.isSchedulingEnabled()) log.debug("Service Plan {} is activated.", getClass().getSimpleName());
        else log.debug("Service Plan {} is NOT activated.", getClass().getSimpleName());

        log.debug("Initialize thread pool: {} ", appConfig.getMaxThreadsPerServicePlanScheduler());
        threadExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(appConfig.getMaxThreadsPerServicePlanScheduler());
    }

    @Override
    public void run() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        String planId = getServicePlanId();
        log.debug("Start run of {} and for plan id {}", getClass().getSimpleName(), planId);

        List<ServiceInstance> serviceInstances = siRepo.findByServicePlanIdAndAppInstanceIndex(planId, appConfig.getCfInstanceIndex(), appConfig.getAmountOfInstances());
        log.debug("For service plan {} there will run {} threads in parallel per application instances " +
                        "to process the work for {} service instances to be processed. ",
                getClass().getSimpleName(), appConfig.getMaxThreadsPerServicePlanScheduler(), serviceInstances.size());

        for(final ServiceInstance si: serviceInstances) {
            log.debug("Check service instance: {}, plan: {}, org: {}, space: {}",
                    si.getServiceInstanceId(),
                    si.getServicePlanId(),
                    si.getOrgId(),
                    si.getSpaceId());

            threadExecutor.execute(() -> {
                try {
                    performActionForServiceInstance(si);
                } catch (IOException e) {
                    log.error("Problem during service plan run.", e);
                }
            });
        }

        // Wait until all threads are finished.
        while (threadExecutor.getActiveCount() > 0) Thread.sleep(1);

        showTime(startTime, serviceInstances.size(), appConfig.getCfInstanceIndex());
    }

    private void showTime(long startTimestamp, int siCount, int appIndex) {

        long d = System.currentTimeMillis() - startTimestamp;
        long dMilli = d % 1000;
        long dSec = (d / 1000) % 60;
        long dMin = d / (1000 * 60);

        log.info("Handled {} registered service instances for service plan {} " +
                "in duration of {}min {}sec {}milli for application index/instance {}",
                siCount, getClass().getSimpleName(), dMin, dSec, dMilli, appIndex);
    }
}
