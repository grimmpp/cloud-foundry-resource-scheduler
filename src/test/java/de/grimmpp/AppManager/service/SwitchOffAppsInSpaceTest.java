package de.grimmpp.AppManager.service;

import de.grimmpp.AppManager.mocks.CfApiMockController;
import de.grimmpp.AppManager.AppManagerApplication;
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
    CfApiMockController cfApiMockController;

    @Autowired
    private ServicePlanSwitchOffAppsInSpace servicePlan;

    @Test
    public void runTest() throws IOException {
        servicePlan.run();

        Assert.assertEquals("PUT", cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD));
        Assert.assertEquals("/v2/apps/15b3885d-0351-4b9b-8697-86641668c123", cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));
        Assert.assertEquals("\"{\\\"state\\\": \\\"STOPPED\\\"}\"", cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }
}
