package grimmpp.AppManager;

import grimmpp.AppManager.model.VcapApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ConfigTest {

    @Autowired
    private VcapApplication vcapApp;

    @Value("${server.port}")
    private Integer serverPort;

    @Test
    public void serverConfigForTestProfileTest() {
        Assert.assertEquals(8111, (int)serverPort);
    }


    @Test
    public void cfApiAddressTest() throws IOException {
        Assert.assertEquals("http://localhost:8111/cf_api_mock", vcapApp.getCf_api());
    }
}
