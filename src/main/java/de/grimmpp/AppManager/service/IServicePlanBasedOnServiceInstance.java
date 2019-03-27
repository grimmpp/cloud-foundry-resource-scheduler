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
    }

    @Scheduled(fixedDelay = 60 * 1000) // 1 min
    public void scheduledRun() throws IOException {
        if (schedulingEnabled) run();
    }

    @Override
    public void run() throws IOException {
        log.debug("Start run of {}", getClass().getSimpleName());

        long startTime = System.currentTimeMillis();
        String planId = getServicePlanId();

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
        log.debug("Duration of Run {}min {}sec {}milli", dMin, dSec, dMilli);
    }
}
