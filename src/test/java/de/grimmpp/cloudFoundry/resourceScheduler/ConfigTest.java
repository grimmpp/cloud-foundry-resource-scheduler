package de.grimmpp.cloudFoundry.resourceScheduler;

import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ConfigTest {

    @Autowired
    private AppConfig appConfig;

    @Value("${server.port}")
    private Integer serverPort;

    @Autowired
    @Qualifier("ProjectLogLevel")
    private String projectLogLevel;

    @Test
    public void logLevelTest() {
        Assert.assertTrue( Arrays.stream(LogLevel.values()).anyMatch(l -> l.toString().equals(projectLogLevel)) );
    }

    @Test
    public void serverConfigForTestProfileTest() {
        Assert.assertEquals(8111, (int)serverPort);
    }


    @Test
    public void cfApiAddressTest() throws IOException {
        Assert.assertEquals("http://localhost:8111/cf_api_mock", appConfig.getVcapApplication().getCf_api());
    }

    @Test
    public void senderAppIdentifyerTest() {
        String headerValue = appConfig.getSenderAppHttpHeaderValue();
        Assert.assertEquals("ae93a4ec-42c2-4087-b4f6-03d79c6aa822:0", headerValue);
    }

    @Test
    public void schedulingFlagTest() {
        // Must be false for testing
        Assert.assertEquals(false, appConfig.isSchedulingEnabled());
    }

    @Test
    public void configuredLoggingLevelTest() {
        // Is set to debug for testing
        Assert.assertEquals("DEBUG", appConfig.getProjectLogLevel());
    }
}
