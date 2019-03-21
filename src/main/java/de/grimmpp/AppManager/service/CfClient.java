package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.model.VcapApplication;
import de.grimmpp.AppManager.model.cfClient.Resource;
import de.grimmpp.AppManager.model.cfClient.Result;
import de.grimmpp.AppManager.model.cfClient.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CfClient {

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper(){{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};

    public static final String URL_PARAMETERS = "?order-direction=asc&results-per-page=100&page=%d";

    public static final String URI_ALL_SERVICE_INSTANCES_OF_A_PLAN = "/v2/service_plans/%s/service_instances";
    public static final String URI_BINDINGS_BY_SERVICE_INSTANCE_ID = "/v2/service_instances/%s/service_bindings";
    public static final String URI_APPS = "/v2/apps";
    public static final String URI_SINGLE_APP = "/v2/apps/%s";
    public static final String URI_APPS_OF_SPACE = "/v2/spaces/%s/apps";


    @Autowired
    private VcapApplication vcapApp;

    public <Entity> List<Resource<Entity>> getResources(String absoluteUrl, Class<Entity> entityType) throws IOException {
        List<Resource<Entity>> resources = new ArrayList<>();

        while (absoluteUrl != null) {
            ResponseEntity<String> resp = restTemplate.getForEntity(absoluteUrl, String.class);

            Result<Entity> result = castRESTResources(resp.getBody(), entityType);
            resources.addAll(result.getResources());

            absoluteUrl = result.getNext_url();
        }

        return resources;
    }

    public <Entity> Entity updateResource(String absoluteUrl, Object payload, Class<Entity> entityType) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        String body = objectMapper.writeValueAsString(payload);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.PUT, request, String.class);

        if (resp.getBody() == null) return null;
        return objectMapper.readValue(resp.getBody(), entityType);
    }

    public <Entity> Resource<Entity> getResource(String absoluteUrl, Class<Entity> entityType) throws IOException {
        Resource<Entity> result = null;

        ResponseEntity<String> resp = restTemplate.getForEntity(absoluteUrl, String.class);
        result = castRESTResource(resp.getBody(), entityType);

        return result;
    }

    public <Entity> Result<Entity> castRESTResources(String result, Class<Entity> entityType) throws IOException {

        JavaType specificType = objectMapper.getTypeFactory().constructParametricType(Result.class, entityType);
        return (Result<Entity>)objectMapper.readValue(result, specificType);
    }

    public <Entity> Resource<Entity> castRESTResource(String result, Class<Entity> entityType) throws IOException {

        JavaType specificType = objectMapper.getTypeFactory().constructParametricType(Resource.class, entityType);
        return (Resource<Entity>)objectMapper.readValue(result, specificType);
    }

    public String buildUrl(String resorucePath, String... args) {
        String url = vcapApp.getCf_api() + resorucePath + String.format(URL_PARAMETERS, 1);
        for(String arg: args) url = String.format(url, arg);

        return url;
    }

    public List<Resource<ServiceInstance>> getServiceInstances(String planId) throws IOException {
        return getResources(buildUrl(URI_ALL_SERVICE_INSTANCES_OF_A_PLAN, planId), ServiceInstance.class);
    }

}
