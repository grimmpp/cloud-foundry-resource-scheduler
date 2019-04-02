package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanSwitchOffAppsInSpaceTest {

    @Autowired
    private Catalog catalog;

    @Autowired
    private ParameterRepository pRepo;

    @Autowired
    private CfApiMockController cfApiMockController;

    @Autowired
    private ServicePlanSwitchOffAppsInSpace servicePlan;

    private String siid = UUID.randomUUID().toString();
    private String spaceId = "162dc5a3-ddb9-41bc-9fb0-38cc7aec73f9";
    private String productionSpaceId = "bc8d3381-390d-4bd7-8c71-25309900a2e3";

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
    public void stopAllAppsTest() throws IOException {
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(spaceId)
                .build();

        pRepo.save(new Parameter(siid, Parameter.KEY_FIXED_DELAY, "5m"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.PUT.toString(), httpMethod);

        String url = "/v2/apps/15b3885d-0351-4b9b-8697-86641668c123";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "{\"state\": \"STOPPED\"}";
        Assert.assertEquals(requestBody, cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }

    @Test
    public void timeTest() throws IOException, ParseException {
        // Create argument
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(spaceId)
                .build();

        // Calculate timespan which is longer so that app won't be stopped.
        String appTime = "2016-06-08";
        long diffTime = System.currentTimeMillis() -  new SimpleDateFormat("yyyy-mm-dd").parse(appTime).getTime();
        diffTime = 2* diffTime / (7 *24 * 60 * 60 * 1000); // in weeks *2

        // Store timespan in DB
        pRepo.save(new Parameter(siid, Parameter.KEY_FIXED_DELAY, diffTime+"w"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.GET.toString(), httpMethod);

        String url = "/v2/spaces/162dc5a3-ddb9-41bc-9fb0-38cc7aec73f9/apps";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "";
        Assert.assertEquals(requestBody, cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }

    @Test
    public void productionSpaceNameTest() throws IOException, ParseException {
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(productionSpaceId)
                .build();

        pRepo.save(new Parameter(siid, Parameter.KEY_FIXED_DELAY, "5m"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.GET.toString(), httpMethod);

        String url = "/v2/spaces/bc8d3381-390d-4bd7-8c71-25309900a2e3";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "";
        Assert.assertEquals(requestBody, cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }
}
