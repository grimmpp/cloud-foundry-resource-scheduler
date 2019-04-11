package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.HttpEndpointSchedulerMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanHttpEndpointSchedulerTest {

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Value("${broker.api.admin-user.username}")
    private String adminUsername;

    @Value("${broker.api.admin-user.password}")
    private String adminPassword;

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServicePlanHttpEndpointScheduler servicePlan;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private HttpEndpointSchedulerMockController mockController;

    @Autowired
    private AppConfig appConfig;

    private String url = "http://localhost:8111/httpEndpointScheduler";

    @Test
    public void catalogTest(){
        boolean b = false;
        for(Plan p: catalog.getServiceDefinitions().get(0).getPlans()) {
            b = p.getId().equals(servicePlan.getServicePlanId());
            if (b) break;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void testSaveParameters1() {
        String siId = UUID.randomUUID().toString();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, url)
                .build();

        servicePlan.saveRequestParamters(request);

        List<Parameter> params = parameterRepository.findByReference(siId);
        Assert.assertEquals(6, params.size());
        Assert.assertEquals("1h", Parameter.getParameterValueByKey(params, Parameter.KEY_FIXED_DELAY));
        Assert.assertEquals(url, Parameter.getParameterValueByKey(params, Parameter.KEY_URL));
        Assert.assertEquals(Boolean.TRUE, Boolean.valueOf(Parameter.getParameterValueByKey(params, Parameter.KEY_SSL_ENABLED)));
        Assert.assertNotNull(Parameter.getParameterValueByKey(params, Parameter.KEY_LAST_CALL));
    }

    @Test
    public void testSaveParameters2() throws JsonProcessingException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8"};
        String headersAsStr = objectMapper.writeValueAsString(headers);
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, url)
                .parameters(Parameter.KEY_HTTP_METHOD, "PUT")
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .parameters(Parameter.KEY_SSL_ENABLED, false)
                .build();

        servicePlan.saveRequestParamters(request);

        List<Parameter> params = parameterRepository.findByReference(siId);
        Assert.assertEquals(6, params.size());
        Assert.assertEquals("1h", Parameter.getParameterValueByKey(params, Parameter.KEY_FIXED_DELAY));
        Assert.assertEquals(url, Parameter.getParameterValueByKey(params, Parameter.KEY_URL));
        Assert.assertEquals("PUT", Parameter.getParameterValueByKey(params, Parameter.KEY_HTTP_METHOD));
        Assert.assertEquals(headersAsStr, Parameter.getParameterValueByKey(params, Parameter.KEY_HTTP_HEADERS));
        Assert.assertEquals(Boolean.FALSE, Boolean.valueOf(Parameter.getParameterValueByKey(params, Parameter.KEY_SSL_ENABLED)));
        Assert.assertNotNull(Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL));
    }

    @Test
    public void testSaveParameters3() {
        String siId = UUID.randomUUID().toString();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters("url", "ftp://localhost/123")
                .build();

        boolean b = false;
        try {
            servicePlan.saveRequestParamters(request);
        } catch(Throwable e) {
            e.printStackTrace();
            b = true;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void testSaveParameters4() throws JsonProcessingException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8"};
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, url)
                .parameters(Parameter.KEY_HTTP_METHOD, "REMOVE")
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        boolean b = false;
        try {
            servicePlan.saveRequestParamters(request);
        } catch(Throwable e) {
            e.printStackTrace();
            b = true;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void testSaveParameters5() throws JsonProcessingException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type-application/json"};
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, url)
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        boolean b = false;
        try {
            servicePlan.saveRequestParamters(request);
        } catch(Throwable e) {
            e.printStackTrace();
            b = true;
        }
        Assert.assertTrue(b);
    }

    @Test
    public void actionTest() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_METHOD, "PUT")
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        servicePlan.saveRequestParamters(request);

        servicePlan.performActionForServiceInstance(si);

        Assert.assertEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        Assert.assertEquals("PUT", mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_HTTP_METHOD));
        Assert.assertEquals(appConfig.getSenderAppHttpHeaderValue(), mockController.getLastOperation(AppConfig.HEADER_NAME_CF_SENDER_APP));
    }

    @Test
    public void actionTest2() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1h")
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        servicePlan.saveRequestParamters(request);

        servicePlan.performActionForServiceInstance(si);

        Assert.assertEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        Assert.assertEquals("GET", mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_HTTP_METHOD));
        Assert.assertEquals(appConfig.getSenderAppHttpHeaderValue(), mockController.getLastOperation(AppConfig.HEADER_NAME_CF_SENDER_APP));
    }


    @Test
    public void actionWithoutTimeExpirationTest() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1s")
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_METHOD, "PUT")
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        servicePlan.saveRequestParamters(request);

        Parameter lastCallParameter = parameterRepository.findByReferenceAndKey(si.getServiceInstanceId(), Parameter.KEY_LAST_CALL);
        lastCallParameter.setValue(Long.toString(System.currentTimeMillis()));
        parameterRepository.save(lastCallParameter);

        servicePlan.performActionForServiceInstance(si);

        if (mockController.lastOperations.size() > 0) {
            Assert.assertNotEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
            Assert.assertEquals(appConfig.getSenderAppHttpHeaderValue(), mockController.getLastOperation(AppConfig.HEADER_NAME_CF_SENDER_APP));
        }
    }

    @Test
    public void timesParameterTest() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test

        long hours = TimeParameterValidator.getHours(System.currentTimeMillis());
        long minutes = TimeParameterValidator.getMinutes(System.currentTimeMillis()-60*1000); //-1min
        String time = hours+":"+minutes;

        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_TIMES, new String[]{time})
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        servicePlan.saveRequestParamters(request);

        servicePlan.performActionForServiceInstance(si);

        Assert.assertEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        Assert.assertEquals("GET", mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_HTTP_METHOD));
        Assert.assertEquals(appConfig.getSenderAppHttpHeaderValue(), mockController.getLastOperation(AppConfig.HEADER_NAME_CF_SENDER_APP));
    }

    @Test
    public void timesParameterWithoutCallTest() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test

        long hours = TimeParameterValidator.getHours(System.currentTimeMillis());
        long minutes = TimeParameterValidator.getMinutes(System.currentTimeMillis()+60*1000); //+1min
        String time = hours+":"+minutes;

        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_TIMES, new String[]{time})
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        servicePlan.saveRequestParamters(request);

        servicePlan.performActionForServiceInstance(si);

        Assert.assertNotEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        Assert.assertEquals(appConfig.getSenderAppHttpHeaderValue(), mockController.getLastOperation(AppConfig.HEADER_NAME_CF_SENDER_APP));
    }

    @Test
    public void saveParameterTwoExclusiveTimeKeys() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", getBasicAuthHeader()};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test

        long hours = TimeParameterValidator.getHours(System.currentTimeMillis());
        long minutes = TimeParameterValidator.getMinutes(System.currentTimeMillis()-60*1000); //-1min
        String time = hours+":"+minutes;

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters(Parameter.KEY_FIXED_DELAY, "1s")
                .parameters(Parameter.KEY_TIMES, new String[]{time})
                .parameters(Parameter.KEY_URL, _url)
                .parameters(Parameter.KEY_HTTP_HEADERS, headers)
                .build();

        boolean b = false;
        try {
            servicePlan.saveRequestParamters(request);
        } catch (Throwable e) {
            b = true;
        }
        Assert.assertTrue(b);
    }




    public String getBasicAuthHeader() {
        return "Authorization: Basic "+Base64.getEncoder().encodeToString((adminUsername+":"+adminPassword).getBytes());
    }
}
