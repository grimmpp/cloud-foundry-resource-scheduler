package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
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

import java.io.IOException;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanAppRestarterTest {

    @Autowired
    private Catalog catalog;

    @Autowired
    private CfApiMockController cfApiMockController;

    @Autowired
    private CfClient cfClient;

    @Autowired
    protected ParameterRepository pRepo;

    @Autowired
    private ServicePlanAppRestarter servicePlan;

    private String siId = UUID.randomUUID().toString();
    private String appId = "ae93a4ec-42c2-4087-b4f6-03d79c6aa822";
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
    public void runTest() throws IOException {
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

        for (int i=0; i<4; i++) {
            String httpMethod = cfApiMockController.lastOperations.get(i).get(CfApiMockController.KEY_HTTP_METHOD);
            Assert.assertEquals(RequestMethod.DELETE.toString(), httpMethod);

            String url = cfApiMockController.lastOperations.get(i).get(CfApiMockController.KEY_URL);
            Assert.assertEquals("/v2/apps/"+appId+"/instances/"+(3-i), url);
        }
    }
}
