package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.helper.ObjectMapperFactory;
import de.grimmpp.AppManager.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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

    protected abstract void performActionForServiceInstance(ServiceInstance si) throws IOException;

    @Override
    public void run() throws IOException {
        String planId = getServicePlanId();

        for(ServiceInstance si: siRepo.findByServicePlanId(planId)) {
            log.debug("Check service instance: {}, plan: {}, org: {}, space: {}",
                    si.getServiceInstanceId(),
                    si.getServicePlanId(),
                    si.getOrgId(),
                    si.getSpaceId());

            performActionForServiceInstance(si);
        }
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return ObjectMapperFactory.getObjectMapper();
    }
}
