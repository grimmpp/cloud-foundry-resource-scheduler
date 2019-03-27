package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.AppManagerApplication;
import de.grimmpp.AppManager.helper.ServicePlanFinder;
import de.grimmpp.AppManager.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
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

    private String appId = "15b3885d-0351-4b9b-8697-86641668c123";

    @Test
    public void basedOnServiceInstanceTest() throws IOException {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String planId = spTestBasedOnSi.planId;
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("time", time);

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
    public void basedOnBidningTest() throws IOException {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String b1Id = UUID.randomUUID().toString();
        String b2Id = UUID.randomUUID().toString();
        String planId = spTestBasedOnB.planId;
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("time", time);

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
                        .key("time")
                        .value(time)
                        .build());

        parameterRepository.save(
                Parameter.builder()
                        .reference(b2Id)
                        .key("time")
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
