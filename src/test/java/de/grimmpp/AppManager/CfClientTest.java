package de.grimmpp.AppManager;

import de.grimmpp.AppManager.model.cfClient.*;
import de.grimmpp.AppManager.service.CfClient;
import de.grimmpp.AppManager.AppManagerApplication;
import de.grimmpp.AppManager.mocks.CfApiMockController;
import de.grimmpp.AppManager.model.cfClient.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CfClientTest {

    @Autowired
    CfClient cfClient;

    @Test
    public void convertJsonTest() throws IOException {

        String restCallResponse = CfApiMockController.getResourceContent("serviceInstancesByPlanId");

        Result<ServiceInstance> result = cfClient.castRESTResources(restCallResponse, ServiceInstance.class);

        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.valueOf(1), result.getTotal_pages());
        Assert.assertEquals(Integer.valueOf(1), result.getTotal_results());
        Assert.assertEquals(1, result.getResources().size());

        Assert.assertEquals("0fac6687-69fd-4567-afb0-dd39503523ff", result.getResources().get(0).getMetadata().getGuid());
        Assert.assertEquals("name-1569", result.getResources().get(0).getEntity().getName());
    }

    @Test
    public void getServiceInstancesByPlanIdTest() throws IOException {

        String servicePlanId = "servicePlanId";
        List<Resource<ServiceInstance>> serviceInstances = cfClient.getServiceInstances(servicePlanId);

        Assert.assertEquals(1, serviceInstances.size());
        Assert.assertEquals("0fac6687-69fd-4567-afb0-dd39503523ff", serviceInstances.get(0).getMetadata().getGuid());
        Assert.assertEquals("name-1569", serviceInstances.get(0).getEntity().getName());
    }

    @Test
    public void getBindingsByServiceInstance() throws IOException {
        String serviceInstanceId = "0fac6687-69fd-4567-afb0-dd39503523ff";
        String url = cfClient.buildUrl(CfClient.URI_BINDINGS_BY_SERVICE_INSTANCE_ID, serviceInstanceId);

        List<Resource<Binding>> bindings = cfClient.getResources(url, Binding.class);

        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals("83a87158-92b2-46ea-be66-9dad6b2cb116", bindings.get(0).getMetadata().getGuid());
    }

    @Test
    public void getParticularApp() throws IOException {
        String appId = "15b3885d-0351-4b9b-8697-86641668c123";
        String url = cfClient.buildUrl(CfClient.URI_SINGLE_APP, appId);
        Resource<Application> app = cfClient.getResource(url, Application.class);

        Assert.assertEquals(appId, app.getMetadata().getGuid());
    }
}
