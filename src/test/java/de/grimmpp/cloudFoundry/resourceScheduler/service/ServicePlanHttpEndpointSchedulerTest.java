package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.test.context.junit4.SpringRunner;

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

    private String url = "http://localhsot:8111/httpEndpointScheduler";

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
        Assert.assertEquals(2, params.size());
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals("1h") && p.getKey().equals("time")));
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals(url) && p.getKey().equals("url")));
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
        Assert.assertEquals(4, params.size());
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals("1h") && p.getKey().equals("time")));
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals(url) && p.getKey().equals("url")));
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals("PUT") && p.getKey().equals("httpMethod")));
        Assert.assertTrue(params.stream().anyMatch(p -> p.getValue().equals(headersAsStr) && p.getKey().equals("httpHeaders")));
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
}
