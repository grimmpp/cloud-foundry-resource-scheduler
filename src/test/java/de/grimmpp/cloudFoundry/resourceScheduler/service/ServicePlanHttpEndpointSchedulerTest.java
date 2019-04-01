package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.HttpEndpointSchedulerMockController;
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
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanHttpEndpointSchedulerTest {

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Autowired
    private Catalog catalog;

    @Autowired
    private ServicePlanHttpEndpointScheduler servicePlan;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private HttpEndpointSchedulerMockController mockController;

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
                .parameters("time", "1h")
                .parameters("url", url)
                .build();

        servicePlan.saveRequestParamters(request);

        List<Parameter> params = parameterRepository.findByReference(siId);
        Assert.assertEquals(3, params.size());
        Assert.assertEquals("1h", Parameter.getParameterValueByKey(params, TimeParameterValidator.KEY));
        Assert.assertEquals(url, Parameter.getParameterValueByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_URL));
        Assert.assertNotNull(Parameter.getParameterValueByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_LAST_CALL));
    }

    @Test
    public void testSaveParameters2() throws JsonProcessingException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8"};
        String headersAsStr = objectMapper.writeValueAsString(headers);
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1h")
                .parameters("url", url)
                .parameters("httpMethod", "PUT")
                .parameters("httpHeaders", headers)
                .build();

        servicePlan.saveRequestParamters(request);

        List<Parameter> params = parameterRepository.findByReference(siId);
        Assert.assertEquals(5, params.size());
        Assert.assertEquals("1h", Parameter.getParameterValueByKey(params, TimeParameterValidator.KEY));
        Assert.assertEquals(url, Parameter.getParameterValueByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_URL));
        Assert.assertEquals("PUT", Parameter.getParameterValueByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_HTTP_METHOD));
        Assert.assertEquals(headersAsStr, Parameter.getParameterValueByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_HTTP_HEADERS));
        Assert.assertNotNull(Parameter.getParameterByKey(params, ServicePlanHttpEndpointScheduler.PARAMETER_KEY_LAST_CALL));
    }

    @Test
    public void testSaveParameters3() {
        String siId = UUID.randomUUID().toString();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1h")
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
        String headersAsStr = objectMapper.writeValueAsString(headers);
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1h")
                .parameters("url", url)
                .parameters("httpMethod", "REMOVE")
                .parameters("httpHeaders", headers)
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
        String headersAsStr = objectMapper.writeValueAsString(headers);
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1h")
                .parameters("url", url)
                .parameters("httpHeaders", headers)
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
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", "Authorization: Basic YWRtaW46YWRtaW4="};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1h")
                .parameters("url", _url)
                .parameters("httpMethod", "PUT")
                .parameters("httpHeaders", headers)
                .build();

        servicePlan.saveRequestParamters(request);

        servicePlan.performActionForServiceInstance(si);

        Assert.assertEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        Assert.assertEquals("PUT", mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_HTTP_METHOD));
    }


    @Test
    public void actionWithoutTimeExpirationTest() throws IOException {
        String siId = UUID.randomUUID().toString();
        String[] headers = new String[]{"Content-Type: application/json", "Accept-Charset: utf-8", "Authorization: Basic YWRtaW46YWRtaW4="};
        String _url = url + "?"+UUID.randomUUID().toString(); // Make url unique in order to check it after the junit test
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siId)
                .build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .planId(servicePlan.getServicePlanId())
                .serviceInstanceId(siId)
                .parameters("time", "1s")
                .parameters("url", _url)
                .parameters("httpMethod", "PUT")
                .parameters("httpHeaders", headers)
                .build();

        servicePlan.saveRequestParamters(request);

        Parameter lastCallParameter = parameterRepository.findByReferenceAndKey(si.getServiceInstanceId(), ServicePlanHttpEndpointScheduler.PARAMETER_KEY_LAST_CALL);
        lastCallParameter.setValue(Long.toString(System.currentTimeMillis()));
        parameterRepository.save(lastCallParameter);

        servicePlan.performActionForServiceInstance(si);

        if (mockController.lastOperations.size() > 0) {
            Assert.assertNotEquals(_url, mockController.getLastOperation(HttpEndpointSchedulerMockController.KEY_URL));
        }
    }
}
