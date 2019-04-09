package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.config.SSLUtil;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.ServiceInstance;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CfClient {

    private RestTemplate restTemplate = null;
    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

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

    public static final String HEADER_NAME_CF_SENDER_APP = "X-CF-SENDER-APP-INSTANCE";

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private VcapApplication vcapApp;

    @Value("${CF_INSTANCE_INDEX}")
    private Integer cfInstanceIndex;

    @Value("${cfClient.SSL-Validation-enabled}")
    private Boolean enableSslValidation;

    @Value("${cfClient.oauth-enabled}")
    private Boolean oauthEnabled;

    @Value("${cfClient.cfApi.username}")
    private String cfApiUsername;

    @Value("${cfClient.cfApi.password}")
    private String cfApiPassword;


    public CfClient() { }

    @PostConstruct
    public void initialize() throws KeyManagementException, NoSuchAlgorithmException {
        log.debug("initialize cfClient");
        if (enableSslValidation) {
            log.debug("Enable SSL!");
            SSLUtil.turnOnSslChecking();
        }
        else {
            log.info("Disable SSL!");
            SSLUtil.turnOffSslChecking();
        }
    }

    private RestTemplate getRestTemplate() {

        if (restTemplate == null) {
            log.debug("Get Token Endpoint: ");
            String tokenEndpoint = getTokenEndpoint();
            log.debug(tokenEndpoint);

            if (!oauthEnabled) {
                log.debug("Create RestTemplate without OAuth2");
                restTemplate = new RestTemplate();

            } else {
                log.debug("Create OAuth2RestTemplate");

                ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
                resource.setAccessTokenUri(tokenEndpoint);
                resource.setClientId("cf");
                resource.setClientSecret("");
                resource.setGrantType("password");
                resource.setUsername(cfApiUsername);
                resource.setPassword(cfApiPassword);

                AccessTokenRequest atr = new DefaultAccessTokenRequest();
                restTemplate = new OAuth2RestTemplate(resource, new DefaultOAuth2ClientContext(atr));
            }
        }

        return restTemplate;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_NAME_CF_SENDER_APP, vcapApp.getApplication_id()+":"+cfInstanceIndex);
        return headers;
    }


    public <Entity> List<Resource<Entity>> getResources(String absoluteUrl, Class<Entity> entityType) throws IOException {
        List<Resource<Entity>> resources = new ArrayList<>();

        while (absoluteUrl != null) {

            ResponseEntity<String> resp = getRestTemplate().exchange(absoluteUrl, HttpMethod.GET, new HttpEntity<>(getHttpHeaders()), String.class);

            Result<Entity> result = castRESTResources(resp.getBody(), entityType);
            resources.addAll(result.getResources());

            absoluteUrl = result.getNext_url();
        }

        return resources;
    }

    public void deleteResource(String absoluteUrl) {
        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(absoluteUrl, HttpMethod.DELETE, request, String.class);
    }

    public <Entity> Entity updateResource(String absoluteUrl, String jsonBody, Class<Entity> entityType) throws IOException {
        HttpEntity<String> request = new HttpEntity<>(jsonBody, getHttpHeaders());
        ResponseEntity<String> resp = getRestTemplate().exchange(absoluteUrl, HttpMethod.PUT, request, String.class);

        if (resp.getBody() == null) return null;
        if (entityType.equals(String.class)) return (Entity) resp.getBody();
        return objectMapper.readValue(resp.getBody(), entityType);
    }

    public <Entity> Resource<Entity> getResource(String absoluteUrl, Class<Entity> entityType) throws IOException {
        Resource<Entity> result = null;

        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = getRestTemplate().exchange(absoluteUrl, HttpMethod.GET, request, String.class);
        result = castRESTResource(resp.getBody(), entityType);

        return result;
    }

    public <Entity> Entity getObject(String absoluteUrl, Class<Entity> entityType) throws IOException {
        HttpEntity<String> request = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> resp = getRestTemplate().exchange(absoluteUrl, HttpMethod.GET, request, String.class);

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
        return buildUrl(resorucePath, true, args);
    }

    public String buildUrl(String resorucePath, boolean enablePaging, String... args) {
        String url = vcapApp.getCf_api() + resorucePath;
        url = String.format(url, args);
        if (enablePaging) url += String.format(URL_PARAMETERS, 1);
        return url;
    }

    public List<Resource<ServiceInstance>> getServiceInstances(String planId) throws IOException {
        return getResources(buildUrl(URI_ALL_SERVICE_INSTANCES_OF_A_PLAN, planId), ServiceInstance.class);
    }

    public String getTokenEndpoint() {
        ApiInfo apiInfo = new RestTemplate().getForEntity(buildUrl(URI_API_INFO), ApiInfo.class).getBody();
        String baseUrl = apiInfo.getToken_endpoint();
        return baseUrl + URI_OAUTH_TOKEN;
    }

    public int getRunningInstanceOfResourceSchedulerApp() throws IOException {
        String url = buildUrl(URI_SINGLE_APP, false, vcapApp.getApplication_id());
        Resource<Application> app = getResource(url, Application.class);
        return app.getEntity().getInstances();
    }
}
