package de.grimmpp.cloudFoundry.resourceScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.service.TimeParameterValidator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TimeParameterValidatorTest {

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    private long s = 1000;
    private long m = 60 * 1000;
    private long h = 60 * 60 * 1000;
    private long d = 24 * 60 * 60 * 1000;
    private long w = 7 * 24 * 60 * 60 * 1000;

    @Test
    public void validationPosFixedDelayTest() {
        boolean b= false;

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w 1d 1h 1m 1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1W 1D 1H 1M 1S");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w, 1d; 1h - 1m_ 1000s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w1d1h1m1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w 1w");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("0w 1w");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("-5h");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w1d; 1h - 1m_ 1s");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1week 2hour 5min 3sec");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1weeks 2hours 5mins 3secs");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("5Minutes 3Seconds");
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("5MinuTe 3Second");
        Assert.assertTrue(b);
    }

    @Test
    public void validationNegFixedDelayTest() {
        boolean b = true;

        b = TimeParameterValidator.validateFixedDelayParameterValue("");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("1w 4a");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("4");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("w");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("0h");
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateFixedDelayParameterValue("a");
        Assert.assertTrue(!b);
    }

    @Test
    public void validatePosTimes() {
        boolean b = false;

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"12:54"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"1:1"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"23:59"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"12:00"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"1:0"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"24:00"});
        Assert.assertTrue(b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"01:01"});
        Assert.assertTrue(b);
    }

    @Test
    public void validateNegTimes() {
        boolean b = true;

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"24:54"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"111:1"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"2:591"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"-5:00"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"0:-5"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"25:00"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"01:60"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"1::5"});
        Assert.assertTrue(!b);

        b = TimeParameterValidator.validateTimesParameterValue(new String[]{"1:1:5"});
        Assert.assertTrue(!b);
    }

    @Test
    public void isFixedDelayExpiredTest() throws IOException {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder()
                .key(Parameter.KEY_FIXED_DELAY)
                .value("10min")
                .build());
        long upTimeInSec = 20 * 60;
        boolean b = false;

        b = TimeParameterValidator.isExpired(parameters, upTimeInSec);
        Assert.assertTrue(b);

        upTimeInSec = 60;
        b = TimeParameterValidator.isExpired(parameters, upTimeInSec);
        Assert.assertTrue(!b);
    }

    @Test
    public void isTimesExpiredTest() throws IOException, ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("H:m dd-MM-yyyy");
        Date date = formatter.parse("12:30 03-04-2019");

        int nowHour = date.getHours();
        int nowMin = date.getMinutes();
        long currentTime = date.getTime() + 60 * 1000; //+1min
        Long lastCall = currentTime - 5 * 60 * 60 * 1000; // -5h
        boolean b = false;

        // lastCall < defined time < current time
        b = TimeParameterValidator.isTimeExpired(currentTime, nowHour, nowMin, lastCall); // doesn't matter for this case
        Assert.assertTrue(b);

        //  defined time < lastCall < current time
        lastCall = currentTime - 30 * 1000;
        b = TimeParameterValidator.isTimeExpired(currentTime, nowHour, nowMin, lastCall); // doesn't matter for this case
        Assert.assertTrue(!b);

        //  defined time < current time < lastCall
        lastCall = currentTime + 30 * 1000;
        b = TimeParameterValidator.isTimeExpired(currentTime, nowHour, nowMin, lastCall); // doesn't matter for this case
        Assert.assertTrue(!b);
    }

    @Test
    public void timeCalcTest() throws Exception {
        long time = 0;

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1s");
        Assert.assertEquals(s, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1m");
        Assert.assertEquals(m, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1h");
        Assert.assertEquals(h, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1d");
        Assert.assertEquals(d, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1w");
        Assert.assertEquals(w, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("1w 1h");
        Assert.assertEquals(w + h, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("5h");
        Assert.assertEquals(5*h, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("3w 5h");
        Assert.assertEquals(3*w + 5*h, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("0w 5h");
        Assert.assertEquals(5*h, time);

        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue("16w 15d 14h 13m 12s");
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
        long time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue(request.getParameters(), defaultTime);
        Assert.assertEquals(w + 3*d + 5*m, time);
        String parameterTime = TimeParameterValidator.getParameterTime(request, defaultTime);
        Assert.assertEquals("1w 3d 5m", parameterTime);

        // Take default time in none is in request
        request.getParameters().remove(Parameter.KEY_FIXED_DELAY);
        b = TimeParameterValidator.containsTimeParameter(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.validateParameterValue(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.doesNotContainOrValidTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        time = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue(request.getParameters(), defaultTime);
        Assert.assertEquals(8*h, time);
        parameterTime = TimeParameterValidator.getParameterTime(request, defaultTime);
        Assert.assertEquals(defaultTime, parameterTime);

        // change to wrong format
        jsonRequest = jsonRequest.replace("\""+Parameter.KEY_FIXED_DELAY+"\": \"1w 3d 5m\"", "\""+Parameter.KEY_FIXED_DELAY+"\": \"a\"");
        request = objectMapper.readValue(jsonRequest, CreateServiceInstanceRequest.class);
        b = TimeParameterValidator.containsTimeParameter(request.getParameters());
        Assert.assertTrue(b);
        b = TimeParameterValidator.validateParameterValue(request.getParameters());
        Assert.assertTrue(!b);
        b = TimeParameterValidator.doesNotContainOrValidTimeParameter(request.getParameters());
        Assert.assertTrue(!b);
        b = false;
        try {
            TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue(request.getParameters(), defaultTime);
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
