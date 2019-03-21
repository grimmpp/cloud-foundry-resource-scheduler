package de.grimmpp.AppManager;

import de.grimmpp.AppManager.controller.BrokerController;
import de.grimmpp.AppManager.model.database.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class BrokerControllerTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private BrokerController brokerController;


    private String siId = UUID.randomUUID().toString();
    private String bId = UUID.randomUUID().toString();
    private String appId = UUID.randomUUID().toString();
    private String planId = UUID.randomUUID().toString();
    private String orgId = UUID.randomUUID().toString();
    private String spaceId = UUID.randomUUID().toString();
    private String time = "1w 3d 5m";


    @Test
    public void serviceInstanceProvisioningTest() {
        cleanDatabase();

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest
                .builder()
                .planId(planId)
                .serviceInstanceId(siId)
                .parameters("time", time)
                .build();

        CreateServiceInstanceResponse response = brokerController.createServiceInstance(request);
        Assert.assertNotNull(response);

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertNotNull(si);
        Assert.assertEquals(siId, si.getServiceInstanceId());
        Assert.assertEquals(planId, si.getServicePlanId());

        List<Parameter> parameterList = parameterRepository.findByReference(siId);
        Assert.assertEquals(1, parameterList.size());
        Assert.assertEquals(siId, parameterList.get(0).getReference());
        Assert.assertEquals("time", parameterList.get(0).getKey());
        Assert.assertEquals(time, parameterList.get(0).getValue());
    }

    @Test
    public void bindingProvisioningTest() {
        serviceInstanceProvisioningTest();

        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
                .bindingId(bId)
                .serviceInstanceId(siId)
                .bindResource(BindResource.builder()
                        .appGuid(appId)
                        .build())
                .parameters("time", time)
                .build();

        brokerController.createServiceInstanceBinding(request);

        Binding b = bindingRepository.findById(bId).get();
        Assert.assertNotNull(b);
        Assert.assertEquals(bId, b.getBindingId());
        Assert.assertEquals(appId, b.getApplicationId());
        Assert.assertEquals(siId, b.getServiceInstanceId());

        List<Parameter> parameterList = parameterRepository.findByReference(bId);
        Assert.assertEquals(1, parameterList.size());
        Assert.assertEquals(bId, parameterList.get(0).getReference());
        Assert.assertEquals("time", parameterList.get(0).getKey());
        Assert.assertEquals(time, parameterList.get(0).getValue());
    }

    @Test
    public void bindingDeprovisioningTest() {
        bindingProvisioningTest();

        DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
                .bindingId(bId)
                .serviceInstanceId(siId)
                .build();

        brokerController.deleteServiceInstanceBinding(request);

        Optional<Binding> b = bindingRepository.findById(bId);
        Assert.assertTrue( !b.isPresent() );

        List<Parameter> parameterList = parameterRepository.findByReference(bId);
        Assert.assertTrue( parameterList.isEmpty() );

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertNotNull(si);
        Assert.assertEquals(siId, si.getServiceInstanceId());
        Assert.assertEquals(planId, si.getServicePlanId());
    }

    @Test
    public void serviceInstanceDeleteTest() {
        bindingDeprovisioningTest();

        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
                .planId(planId)
                .serviceInstanceId(siId)
                .build();

        brokerController.deleteServiceInstance(request);

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertTrue(si == null);

        List<Parameter> parameterList = parameterRepository.findByReference(siId);
        Assert.assertTrue( parameterList.isEmpty() );
    }

    @Test
    public void updateServiceInstanceTest(){
        serviceInstanceProvisioningTest();

        String newTime = "3d 5m";

        UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
                .planId(planId)
                .serviceInstanceId(siId)
                .parameters("time", newTime)
                .build();

        brokerController.updateServiceInstance(request);

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertNotNull(si);
        Assert.assertEquals(siId, si.getServiceInstanceId());
        Assert.assertEquals(planId, si.getServicePlanId());

        List<Parameter> parameterList = parameterRepository.findByReference(siId);
        boolean containsParameterTime = false;
        for(Parameter p: parameterList) {
            if (p.getKey().equals("time")) {
                containsParameterTime = true;
                Assert.assertEquals(newTime, p.getValue());
            }
        }
        Assert.assertTrue( containsParameterTime );
    }

    public void cleanDatabase() {
        serviceInstanceRepository.deleteAll();
        bindingRepository.deleteAll();
        parameterRepository.deleteAll();
    }
}
