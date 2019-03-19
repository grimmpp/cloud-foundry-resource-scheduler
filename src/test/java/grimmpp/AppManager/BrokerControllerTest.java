package grimmpp.AppManager;

import grimmpp.AppManager.controller.BrokerController;
import grimmpp.AppManager.model.database.ServiceInstance;
import grimmpp.AppManager.model.database.ServiceInstanceRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class BrokerControllerTest {


    @Autowired
    private BrokerController brokerController;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Test
    public void serviceInstanceProvisioningTest() {
        String siId = UUID.randomUUID().toString();
        String planId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";


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
        Assert.assertEquals(time, si.getPropertyTime());
    }
}
