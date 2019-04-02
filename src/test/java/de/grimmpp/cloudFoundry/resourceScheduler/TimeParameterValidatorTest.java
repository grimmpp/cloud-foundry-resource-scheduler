package de.grimmpp.cloudFoundry.resourceScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.service.TimeParameterValidator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import java.io.IOException;

public class TimeParameterValidatorTest {

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    private long s = 1000;
    private long m = 60 * 1000;
    private long h = 60 * 60 * 1000;
    private long d = 24 * 60 * 60 * 1000;
    private long w = 7 * 24 * 60 * 60 * 1000;

    @Test
    public void validationPosTest() {
        boolean b= false;

        b = TimeParameterValidator.validateParameterValue("1w 1d 1h 1m 1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1W 1D 1H 1M 1S");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1w, 1d; 1h - 1m_ 1000s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1w1d1h1m1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1w 1w");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("0w 1w");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("-5h");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1w1d; 1h - 1m_ 1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1week 2hour 5min 3sec");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("1weeks 2hours 5mins 3secs");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("5Minutes 3Seconds");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateParameterValue("5MinuTe 3Second");
        Assert.assertTrue(b);
    }

    @Test
    public void validationNegTest() {
        boolean b = true;

        b = TimeParameterValidator.validateParameterValue("");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateParameterValue("1w 4a");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateParameterValue("4");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateParameterValue("w");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateParameterValue("0h");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateParameterValue("a");
        Assert.assertTrue(!b);
    }

    @Test
    public void timeCalcTest() throws Exception {
        long time = 0;

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1s");
        Assert.assertEquals(s, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1m");
        Assert.assertEquals(m, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1h");
        Assert.assertEquals(h, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1d");
        Assert.assertEquals(d, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1w");
        Assert.assertEquals(w, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("1w 1h");
        Assert.assertEquals(w + h, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("5h");
        Assert.assertEquals(5*h, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("3w 5h");
        Assert.assertEquals(3*w + 5*h, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("0w 5h");
        Assert.assertEquals(5*h, time);

        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue("16w 15d 14h 13m 12s");
        Assert.assertEquals(16*w + 15*d + 14*h + 13*m + 12*s, time);
    }

    @Test
    public void defaultTimeTest() throws IOException {
        String jsonRequest = CfApiMockController.getResourceContent("serviceInstanceProvisioningRequest_simple");
        CreateServiceInstanceRequest request = objectMapper.readValue(jsonRequest, CreateServiceInstanceRequest.class); //"time;1w 3d 5m"

        String defaultTime = "8h";

        // Take time from request
        boolean b = TimeParameterValidator.containsTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        b = TimeParameterValidator.validateParameterValue(request.getParameters());
        Assert.assertTrue(b);
        b = TimeParameterValidator.doesNotContainOrValidTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        long time = TimeParameterValidator.getTimeInMilliSecFromParameterValue(request.getParameters(), defaultTime);
        Assert.assertEquals(w + 3*d + 5*m, time);
        String parameterTime = TimeParameterValidator.getParameterTime(request, defaultTime);
        Assert.assertEquals("1w 3d 5m", parameterTime);

        // Take default time in none is in request
        request.getParameters().remove(TimeParameterValidator.KEY_FIXED_DELAY);
        b = TimeParameterValidator.containsTimeParameter(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.validateParameterValue(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.doesNotContainOrValidTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        time = TimeParameterValidator.getTimeInMilliSecFromParameterValue(request.getParameters(), defaultTime);
        Assert.assertEquals(8*h, time);
        parameterTime = TimeParameterValidator.getParameterTime(request, defaultTime);
        Assert.assertEquals(defaultTime, parameterTime);

        // change to wrong format
        jsonRequest = jsonRequest.replace("\""+TimeParameterValidator.KEY_FIXED_DELAY+"\": \"1w 3d 5m\"", "\""+TimeParameterValidator.KEY_FIXED_DELAY+"\": \"a\"");
        request = objectMapper.readValue(jsonRequest, CreateServiceInstanceRequest.class);
        b = TimeParameterValidator.containsTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        b = TimeParameterValidator.validateParameterValue(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.doesNotContainOrValidTimeParameter(request.getParameters());
        Assert.assertTrue(!b);
        b = false;
        try {
            TimeParameterValidator.getTimeInMilliSecFromParameterValue(request.getParameters(), defaultTime);
        } catch (Throwable e) {
            b = true;
        }
        Assert.assertTrue(b);
        b = false;
        try {
            TimeParameterValidator.getParameterTime(request, defaultTime);
        } catch (Throwable e) {
            b = true;
        }
        Assert.assertTrue(b);
    }

}
