package grimmpp.AppManager.serviceplans;

import grimmpp.AppManager.AppManagerApplication;
import grimmpp.AppManager.mocks.MockController;
import grimmpp.AppManager.service.ServicePlanSwitchOffAppsInSpace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SwitchOffAppsInSpaceTest {

    @Autowired
    MockController mockController;

    @Autowired
    private ServicePlanSwitchOffAppsInSpace servicePlan;

    @Test
    public void runTest() throws IOException {
        servicePlan.run();

        Assert.assertEquals("PUT", mockController.lastOperations.get(0).get("HttpMethod"));
        Assert.assertEquals("/v2/apps/15b3885d-0351-4b9b-8697-86641668c123", mockController.lastOperations.get(0).get("URI"));
        Assert.assertEquals("\"{\\\"state\\\": \\\"STOPPED\\\"}\"", mockController.lastOperations.get(0).get("Body"));
    }
}
