package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.AbstractMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Application;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Resource;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Binding;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.tags.Param;

import java.io.IOException;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanRollingContainerRestarterTest {

    @Autowired
    private CfApiMockController cfApiMockController;

    @Autowired
    private CfClient cfClient;

    @Autowired
    private Catalog catalog;

    @Autowired
    protected ParameterRepository pRepo;

    @Autowired
    private ServicePlanRollingContainerRestarter servicePlan;

    private String siId = UUID.randomUUID().toString();
    private String spaceId = "359b04a4-1006-4c57-b14d-9dfec46f8e78";

    @Test
    public void catalogTest() {
        boolean b = false;
        for(Plan p: catalog.getServiceDefinitions().get(0).getPlans()) {
            b = p.getId().equals(servicePlan.getServicePlanId());
            if (b) break;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void saveRequestParamtersTest() {
        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
                .serviceInstanceId(siId)
                .planId(ServicePlanRollingContainerRestarter.PLAN_ID)
                .parameters(Parameter.KEY_FIXED_DELAY, "1d")
                .build();
        servicePlan.saveRequestParamters(request);

        String value = pRepo.findByReferenceAndKey(siId, Parameter.KEY_FIXED_DELAY).getValue();
        Assert.assertEquals("1d", value);
    }

    @Test
    public void brokenContainersNegTest() throws IOException {
        String appId = "ae93a4ec-42c2-4087-b4f6-03d79c6aa822";
        // for this test not all containers are healthy but app is running
        // expected is that app should be shutdown but won't because not all containers are healthy.
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .servicePlanId(ServicePlanAppRestarter.PLAN_ID)
                .orgId("")
                .spaceId(spaceId)
                .build();

        Binding b = Binding.builder()
                .bindingId(UUID.randomUUID().toString())
                .serviceInstanceId(siId)
                .applicationId(appId)
                .build();

        String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
        Resource<Application> app = cfClient.getResource(appUrl, Application.class);

        long time = 10 * 1000;  //10sec

        servicePlan.performActionForBinding(si, b, app, time);

        Assert.assertEquals(RequestMethod.GET.toString(), cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD));
        String aiUrl = AbstractMockController.BASE_URL + "/v2/apps/ae93a4ec-42c2-4087-b4f6-03d79c6aa822/instances";
        Assert.assertTrue(aiUrl.endsWith(cfApiMockController.getLastOperation(CfApiMockController.KEY_URL)));
    }

    @Test
    public void allAIsAreHealthyTest() throws IOException {
        String appId = "187f4a00-58b2-11e9-8647-d663bd873d93";

        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .servicePlanId(ServicePlanAppRestarter.PLAN_ID)
                .orgId("")
                .spaceId(spaceId)
                .build();

        Binding b = Binding.builder()
                .bindingId(UUID.randomUUID().toString())
                .serviceInstanceId(siId)
                .applicationId(appId)
                .build();

        String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
        Resource<Application> app = cfClient.getResource(appUrl, Application.class);

        long time = 10 * 1000;  //10sec

        servicePlan.performActionForBinding(si, b, app, time);

        Assert.assertEquals(RequestMethod.DELETE.toString(), cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD));
        String aiUrl = AbstractMockController.BASE_URL + "/v2/apps/187f4a00-58b2-11e9-8647-d663bd873d93/instances/1";
        Assert.assertEquals(aiUrl, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));
    }

    @Test
    public void allAIsAreHealthyButTimeNotExpiredTest() throws IOException {
        String appId = "187f4a00-58b2-11e9-8647-d663bd873d93";

        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .servicePlanId(ServicePlanAppRestarter.PLAN_ID)
                .orgId("")
                .spaceId(spaceId)
                .build();

        Binding b = Binding.builder()
                .bindingId(UUID.randomUUID().toString())
                .serviceInstanceId(siId)
                .applicationId(appId)
                .build();

        String appUrl = cfClient.buildUrl(CfClient.URI_SINGLE_APP, b.getApplicationId());
        Resource<Application> app = cfClient.getResource(appUrl, Application.class);

        long time = 1000 * 24 * 60 * 60 * 1000;  //100 days

        servicePlan.performActionForBinding(si, b, app, time);

        Assert.assertEquals(RequestMethod.GET.toString(), cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD));
        String aiUrl = AbstractMockController.BASE_URL + "/v2/apps/187f4a00-58b2-11e9-8647-d663bd873d93/instances";
        Assert.assertEquals(aiUrl, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));
    }
}
