package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.mocks.CfApiMockController;
import de.grimmpp.AppManager.AppManagerApplication;
import de.grimmpp.AppManager.model.database.Parameter;
import de.grimmpp.AppManager.model.database.ParameterRepository;
import de.grimmpp.AppManager.model.database.ServiceInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SwitchOffAppsInSpaceTest {

    @Autowired
    private ParameterRepository pRepo;

    @Autowired
    private CfApiMockController cfApiMockController;

    @Autowired
    private ServicePlanSwitchOffAppsInSpace servicePlan;

    private String siid = UUID.randomUUID().toString();
    private String spaceId = "162dc5a3-ddb9-41bc-9fb0-38cc7aec73f9";

    @Test
    public void stopAllAppsTest() throws IOException {
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(spaceId)
                .build();

        pRepo.save(new Parameter(siid, TimeParameterValidator.KEY, "5m"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.PUT.toString(), httpMethod);

        String url = "/v2/apps/15b3885d-0351-4b9b-8697-86641668c123";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "\"{\\\"state\\\": \\\"STOPPED\\\"}\"";
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
        pRepo.save(new Parameter(siid, TimeParameterValidator.KEY, diffTime+"w"));

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
}
