package de.grimmpp.cloudFoundry.resourceScheduler;

import com.zaxxer.hikari.HikariDataSource;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.service.ServicePlanRollingContainerRestarter;
import de.grimmpp.cloudFoundry.resourceScheduler.service.TimeParameterValidator;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DatabaseTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    DataSource dataSource;

    @Test
    public void datasourceTest() {
        Assert.assertNotNull(dataSource);

        Assert.assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hds = (HikariDataSource)dataSource;

        Assert.assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1",hds.getJdbcUrl());
        Assert.assertEquals("org.h2.Driver",hds.getDriverClassName());
        Assert.assertEquals("HikariPool-1",hds.getPoolName());
        Assert.assertEquals("sa",hds.getUsername());
        Assert.assertEquals("sa",hds.getPassword());
    }

    @Test
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void ServiceInstanceAndParamterTest() {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String planId = UUID.randomUUID().toString();
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

        List<ServiceInstance> siList = Lists.newArrayList(serviceInstanceRepository.findAll());
        Assert.assertEquals(1, siList.size());
        Assert.assertEquals(id, siList.get(0).getServiceInstanceId());
        Assert.assertEquals(planId, siList.get(0).getServicePlanId());
        Assert.assertEquals(spaceId, siList.get(0).getSpaceId());
        Assert.assertEquals(orgId, siList.get(0).getOrgId());

        parameterRepository.save(
                Parameter.builder()
                .reference(id)
                .key(Parameter.KEY_FIXED_DELAY)
                .value(time)
                .build());
        parameterRepository.saveAll(Parameter.convert("_"+id, parameters));

        List<Parameter> parameterList = Lists.newArrayList(parameterRepository.findAll());
        Assert.assertEquals(2, parameterList.size());

        parameterList = parameterRepository.findByReference(id);
        Assert.assertEquals(1, parameterList.size());
    }

    @Test
    public void BindingTest() {
        cleanDatabase();

        String serviceInstanceId = UUID.randomUUID().toString();
        String appId = UUID.randomUUID().toString();
        String bindingId = UUID.randomUUID().toString();

        Binding b = Binding.builder()
                .applicationId(appId)
                .bindingId(bindingId)
                .serviceInstanceId(serviceInstanceId)
                .build();

        bindingRepository.save(b);
        Binding b2 = bindingRepository.findById(bindingId).get();
        Assert.assertNotNull(b2);
        Assert.assertEquals(bindingId, b2.getBindingId());
        Assert.assertEquals(appId, b2.getApplicationId());
        Assert.assertEquals(serviceInstanceId, b2.getServiceInstanceId());
    }

    @Test
    public void appInstanceDataSegmentationTest() {
        cleanDatabase();

        String planId = UUID.randomUUID().toString();

        int serviceCount = 12;
        for (int i=0;i<serviceCount;i++) createRandomServiceInstance(planId);

        int index = 0;
        int amountOfContainers = 1;
        List<ServiceInstance> sis = serviceInstanceRepository.findByServicePlanIdAndAppInstanceIndex(planId, index, amountOfContainers);
        Assert.assertEquals(serviceCount/amountOfContainers, sis.size());
        for (ServiceInstance si: sis) Assert.assertEquals(index, si.getId() % amountOfContainers);

        index = 0;
        amountOfContainers = 2;
        sis = serviceInstanceRepository.findByServicePlanIdAndAppInstanceIndex(planId, index, amountOfContainers);
        Assert.assertEquals(serviceCount/amountOfContainers, sis.size());
        for (ServiceInstance si: sis) Assert.assertEquals(index, si.getId() % amountOfContainers);

        index = 2;
        amountOfContainers = 3;
        sis = serviceInstanceRepository.findByServicePlanIdAndAppInstanceIndex(planId, index, amountOfContainers);
        Assert.assertEquals(serviceCount/amountOfContainers, sis.size());
        for (ServiceInstance si: sis) Assert.assertEquals(index, si.getId() % amountOfContainers);
    }

    private void createRandomServiceInstance(String servicePlan) {
        serviceInstanceRepository.save(
            ServiceInstance.builder()
                    .serviceInstanceId(UUID.randomUUID().toString())
                    .orgId(UUID.randomUUID().toString())
                    .spaceId(UUID.randomUUID().toString())
                    .servicePlanId(servicePlan)
                    .build());
    }

    public void cleanDatabase() {
        serviceInstanceRepository.deleteAll();
        bindingRepository.deleteAll();
        parameterRepository.deleteAll();
    }
}
