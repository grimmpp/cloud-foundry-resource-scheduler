package de.grimmpp.cloudFoundry.resourceScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.controller.BrokerController;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.service.TimeParameterValidator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class BrokerControllerTest {

    private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Autowired
    private BrokerController brokerController;


    private String siId = "8b07d88e-ac43-4a3a-93dc-cf54b389a541";
    private String bId = "b9e2d70e-80b7-4784-8e67-0a4b0bccbb87";
    private String appId = "ae93a4ec-42c2-4087-b4f6-03d79c6aa822";
    private String planId = "4e1020f9-6577-4ba3-885f-95bb978b4939";
    private String orgId = "701e1662-9fc4-4842-a798-1c9992e435c9";
    private String spaceId = "359b04a4-1006-4c57-b14d-9dfec46f8e78";
    private String time = "1w 3d 5m";


    @Test
    public void serviceInstanceProvisioningTest() throws IOException {
        cleanDatabase();

        CreateServiceInstanceRequest request = CfApiMockController.getResourceContent(
                "serviceInstanceProvisioningRequest_simple",
                CreateServiceInstanceRequest.class);

        CreateServiceInstanceResponse response = brokerController.createServiceInstance(request);
        Assert.assertNotNull(response);

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertNotNull(si);
        Assert.assertEquals(siId, si.getServiceInstanceId());
        Assert.assertEquals(planId, si.getServicePlanId());
        Assert.assertEquals(orgId, si.getOrgId());
        Assert.assertEquals(spaceId, si.getSpaceId());

        List<Parameter> parameterList = parameterRepository.findByReference(siId);
        Assert.assertEquals(1, parameterList.size());
        Assert.assertEquals(siId, parameterList.get(0).getReference());
        Assert.assertEquals(Parameter.KEY_FIXED_DELAY, parameterList.get(0).getKey());
        Assert.assertEquals(time, parameterList.get(0).getValue());
    }

    @Test
    public void serviceInstanceProvisioningWrongTimeFormatTest() throws IOException {
        cleanDatabase();

        String jsonRequest = CfApiMockController.getResourceContent("serviceInstanceProvisioningRequest_simple").replace("1w 3d 5m", "wrong time format");
        CreateServiceInstanceRequest request = objectMapper.readValue(jsonRequest, CreateServiceInstanceRequest.class); //"time;1w 3d 5m"

        boolean b = false;
        try {
            brokerController.createServiceInstance(request);
        } catch (Throwable e) {
            b = true;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void bindingProvisioningTest() throws IOException {
        serviceInstanceProvisioningTest();

        CreateServiceInstanceBindingRequest request = CfApiMockController.getResourceContent(
                "bindingRequest_simple",
                CreateServiceInstanceBindingRequest.class);

        brokerController.createServiceInstanceBinding(request);

        Binding b = bindingRepository.findById(bId).get();
        Assert.assertNotNull(b);
        Assert.assertEquals(bId, b.getBindingId());
        Assert.assertEquals(appId, b.getApplicationId());
        Assert.assertEquals(siId, b.getServiceInstanceId());

        List<Parameter> parameterList = parameterRepository.findByReference(bId);
        Assert.assertEquals(1, parameterList.size());
        Assert.assertEquals(bId, parameterList.get(0).getReference());
        Assert.assertEquals(Parameter.KEY_FIXED_DELAY, parameterList.get(0).getKey());
        Assert.assertEquals(time, parameterList.get(0).getValue());
    }

    @Test
    public void bindingDeprovisioningTest() throws IOException {
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
    public void serviceInstanceDeleteTest() throws IOException {
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
    public void updateServiceInstanceTest() throws IOException {
        serviceInstanceProvisioningTest();

        String newTime = "3d 5m";

        UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
                .planId(planId)
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, newTime)
                .build();

        brokerController.updateServiceInstance(request);

        ServiceInstance si = serviceInstanceRepository.findByServiceInstanceId(siId);
        Assert.assertNotNull(si);
        Assert.assertEquals(siId, si.getServiceInstanceId());
        Assert.assertEquals(planId, si.getServicePlanId());

        List<Parameter> parameterList = parameterRepository.findByReference(siId);
        boolean containsParameterTime = false;
        for(Parameter p: parameterList) {
            if (p.getKey().equals(Parameter.KEY_FIXED_DELAY)) {
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
