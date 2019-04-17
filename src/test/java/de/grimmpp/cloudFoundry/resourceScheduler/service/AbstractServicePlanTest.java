package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ServicePlanFinder;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AbstractServicePlanTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private ServicePlanTestBasedOnSi spTestBasedOnSi;

    @Autowired
    private ServicePlanTestBasedOnBinding spTestBasedOnB;

    @Autowired
    private CfClient cfClient;

    @Autowired
    private AppConfig appConfig;

    private String appId = "15b3885d-0351-4b9b-8697-86641668c123";

    @Before
    public void init() throws IOException {
        appConfig.updateAmountOfInstances(
                cfClient.getRunningInstanceOfResourceSchedulerApp() );

        Assert.assertEquals(1L, (long)appConfig.getAmountOfInstances());
    }

    @Test
    public void basedOnServiceInstanceTest() throws IOException, InterruptedException {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String planId = spTestBasedOnSi.planId;
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        Map<String,Object> parameters = new HashMap<>();
        parameters.put(Parameter.KEY_FIXED_DELAY, time);

        serviceInstanceRepository.save(
                ServiceInstance.builder()
                        .serviceInstanceId(id)
                        .orgId(orgId)
                        .spaceId(spaceId)
                        .servicePlanId(planId)
                        .build());

        spTestBasedOnSi.run();

        Assert.assertEquals(spTestBasedOnSi.planId, ServicePlanFinder.findServicePlan(spTestBasedOnSi.planId).getServicePlanId());
        Assert.assertEquals(1, spTestBasedOnSi.runCount);
    }

    @Test
    public void basedOnBidningTest() throws IOException, InterruptedException {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String b1Id = UUID.randomUUID().toString();
        String b2Id = UUID.randomUUID().toString();
        String planId = spTestBasedOnB.planId;
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        Map<String,Object> parameters = new HashMap<>();
        parameters.put(Parameter.KEY_FIXED_DELAY, time);

        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(id)
                .orgId(orgId)
                .spaceId(spaceId)
                .servicePlanId(planId)
                .build();
        serviceInstanceRepository.save(si);

        Binding b1 = Binding.builder()
                .applicationId(appId)
                .bindingId(b1Id)
                .serviceInstanceId(id)
                .build();
        bindingRepository.save(b1);

        Binding b2 = Binding.builder()
                .applicationId(appId)
                .bindingId(b2Id)
                .serviceInstanceId(id)
                .build();
        bindingRepository.save(b2);

        parameterRepository.save(
                Parameter.builder()
                        .reference(b1Id)
                        .key(Parameter.KEY_FIXED_DELAY)
                        .value(time)
                        .build());

        parameterRepository.save(
                Parameter.builder()
                        .reference(b2Id)
                        .key(Parameter.KEY_FIXED_DELAY)
                        .value(time)
                        .build());

        spTestBasedOnB.run();

        Assert.assertEquals(spTestBasedOnB.planId, ServicePlanFinder.findServicePlan(spTestBasedOnB.planId).getServicePlanId());
        Assert.assertEquals(2, spTestBasedOnB.runCount);
    }

    public void cleanDatabase() {
        serviceInstanceRepository.deleteAll();
        bindingRepository.deleteAll();
        parameterRepository.deleteAll();
    }
}
