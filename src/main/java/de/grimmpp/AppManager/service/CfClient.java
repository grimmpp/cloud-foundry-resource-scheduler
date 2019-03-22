package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.model.VcapApplication;
import de.grimmpp.AppManager.model.cfClient.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CfClient {

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper(){{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};
    private OAuthExchange oAuthExchange = OAuthExchange.builder().access_token("token").build();

    public static final String URL_PARAMETERS = "?order-direction=asc&results-per-page=100&page=%d";

    public static final String URI_ALL_SERVICE_INSTANCES_OF_A_PLAN = "/v2/service_plans/%s/service_instances";
    public static final String URI_BINDINGS_BY_SERVICE_INSTANCE_ID = "/v2/service_instances/%s/service_bindings";
    public static final String URI_APPS = "/v2/apps";
    public static final String URI_SINGLE_APP = "/v2/apps/%s";
    public static final String URI_APPS_OF_SPACE = "/v2/spaces/%s/apps";
    public static final String URI_APP_INSTANCE = "/v2/apps/%s/instances/%s";
    public static final String URI_APP_INSTANCES = "/v2/apps/%s/instances";
    public static final String URI_SINGLE_SPACE = "/v2/spaces/%s";
    public static final String URI_OAUTH_TOKEN = "/oauth/token";
    public static final String URI_API_INFO = "/v2/info";


    @Autowired
    private VcapApplication vcapApp;


    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(oAuthExchange.getAccess_token());
        return headers;
    }


    public <Entity> List<Resource<Entity>> getResources(String absoluteUrl, Class<Entity> entityType) throws IOException {
        List<Resource<Entity>> resources = new ArrayList<>();

        while (absoluteUrl != null) {

            ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.GET, new HttpEntity<>(getHttpHeaders()), String.class);

            Result<Entity> result = castRESTResources(resp.getBody(), entityType);
            resources.addAll(result.getResources());

            absoluteUrl = result.getNext_url();
        }

        return resources;
    }

    public void deleteResource(String absoluteUrl) throws IOException {
        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.DELETE, request, String.class);
    }

    public <Entity> Entity updateResource(String absoluteUrl, Object payload, Class<Entity> entityType) throws IOException {
        String body = objectMapper.writeValueAsString(payload);

        HttpEntity<String> request = new HttpEntity<>(body, getHttpHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.PUT, request, String.class);

        if (resp.getBody() == null) return null;
        return objectMapper.readValue(resp.getBody(), entityType);
    }

    public <Entity> Resource<Entity> getResource(String absoluteUrl, Class<Entity> entityType) throws IOException {
        Resource<Entity> result = null;

        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.GET, request, String.class);
        result = castRESTResource(resp.getBody(), entityType);

        return result;
    }

    public <Entity> Entity getObject(String absoluteUrl, Class<Entity> entityType) throws IOException {
        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.GET, request, String.class);

        return objectMapper.readValue(resp.getBody(), entityType);
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
        url = String.format(url, args);

        return url;
    }

    public List<Resource<ServiceInstance>> getServiceInstances(String planId) throws IOException {
        return getResources(buildUrl(URI_ALL_SERVICE_INSTANCES_OF_A_PLAN, planId), ServiceInstance.class);
    }

    public String getTokenEndpoint() throws IOException {
        ApiInfo apiInfo = restTemplate.getForEntity(buildUrl(URI_API_INFO), ApiInfo.class).getBody();
        String baseUrl = apiInfo.getToken_endpoint();
        return baseUrl + URI_OAUTH_TOKEN;
    }

    public OAuthExchange getAccessToken() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("AUTHORIZATION", "Basic Y2Y6");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponents uri = UriComponentsBuilder.fromHttpUrl(getTokenEndpoint())
                .queryParam("username", URLEncoder.encode("username", StandardCharsets.UTF_8.toString())) //TODO: enter user
                .queryParam("password", URLEncoder.encode("password", StandardCharsets.UTF_8.toString())) //TODO: enter password
                .queryParam("grant_type", "password")
                .build(true);

        ResponseEntity<OAuthExchange> resp = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, OAuthExchange.class);
        return resp.getBody();
    }
}
