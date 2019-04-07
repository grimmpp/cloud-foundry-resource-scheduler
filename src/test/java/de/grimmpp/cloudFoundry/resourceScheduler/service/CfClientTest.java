package de.grimmpp.cloudFoundry.resourceScheduler.service;

import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.*;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.Binding;
import de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient.ServiceInstance;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CfClientTest {

    @Autowired
    private VcapApplication vcapApp;

    @Autowired
    CfClient cfClient;

    @Value("${cfClient.SSL-Validation-enabled}")
    private Boolean enableSslValidation;

    @Value("${cfClient.oauth-enabled}")
    private Boolean oauthEnabled;

    @Value("${CF_INSTANCE_INDEX}")
    private Integer cfInstanceIndex;

    @Autowired
    private CfApiMockController cfApiMockController;

    @Test
    public void isSslValidationDisabledForTest(){
        Assert.assertTrue(!enableSslValidation);
    }

    @Test
    public void isOauthDisabledForTesting() {
        Assert.assertTrue(!oauthEnabled);
    }

    @Test
    public void convertJsonTest() throws IOException {

        String restCallResponse = CfApiMockController.getResourceContent("serviceInstancesByPlanId");

        Result<ServiceInstance> result = cfClient.castRESTResources(restCallResponse, ServiceInstance.class);

        Assert.assertNotNull(result);
        Assert.assertEquals(Integer.valueOf(1), result.getTotal_pages());
        Assert.assertEquals(Integer.valueOf(1), result.getTotal_results());
        Assert.assertEquals(1, result.getResources().size());

        Assert.assertEquals("0fac6687-69fd-4567-afb0-dd39503523ff", result.getResources().get(0).getMetadata().getGuid());
        Assert.assertEquals("name-1569", result.getResources().get(0).getEntity().getName());
    }

    @Test
    public void getServiceInstancesByPlanIdTest() throws IOException {

        String servicePlanId = "servicePlanId";
        List<Resource<ServiceInstance>> serviceInstances = cfClient.getServiceInstances(servicePlanId);

        Assert.assertEquals(1, serviceInstances.size());
        Assert.assertEquals("0fac6687-69fd-4567-afb0-dd39503523ff", serviceInstances.get(0).getMetadata().getGuid());
        Assert.assertEquals("name-1569", serviceInstances.get(0).getEntity().getName());
    }

    @Test
    public void getBindingsByServiceInstance() throws IOException {
        String serviceInstanceId = "0fac6687-69fd-4567-afb0-dd39503523ff";
        String url = cfClient.buildUrl(CfClient.URI_BINDINGS_BY_SERVICE_INSTANCE_ID, serviceInstanceId);

        List<Resource<Binding>> bindings = cfClient.getResources(url, Binding.class);

        Assert.assertEquals(1, bindings.size());
        Assert.assertEquals("83a87158-92b2-46ea-be66-9dad6b2cb116", bindings.get(0).getMetadata().getGuid());
    }

    @Test
    public void getParticularApp() throws IOException {
        String appId = "15b3885d-0351-4b9b-8697-86641668c123";
        String url = cfClient.buildUrl(CfClient.URI_SINGLE_APP, appId);
        Resource<Application> app = cfClient.getResource(url, Application.class);

        Assert.assertEquals(appId, app.getMetadata().getGuid());
    }

    @Test
    public void getApiInfo() throws IOException {
        ApiInfo apiInfo = cfClient.getObject(cfClient.buildUrl(CfClient.URI_API_INFO), ApiInfo.class);
        Assert.assertEquals("http://localhost:8111/cf_api_mock", apiInfo.getToken_endpoint());

        String url = cfClient.getTokenEndpoint();
        Assert.assertEquals("http://localhost:8111/cf_api_mock/oauth/token", url);
    }

    @Test
    public void appSenderHeaderTest() throws IOException {
        ApiInfo apiInfo = cfClient.getObject(cfClient.buildUrl(CfClient.URI_API_INFO), ApiInfo.class);
        String senderId = cfApiMockController.getLastOperation(CfClient.HEADER_NAME_CF_SENDER_APP);

        Assert.assertEquals(vcapApp.getApplication_id()+":"+cfInstanceIndex, senderId);
    }

/*
    @Test
    public void authorizationTest() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        //vcapApp.setCf_api("https://api.dev.cfdev.sh");
        // "https://uaa.dev.cfdev.sh/oauth/token"

        //OAuthExchange oauth = cfClient.getAccessToken("https://uaa.dev.cfdev.sh");

        ResponseEntity<ApiInfo> apiInfo = new RestTemplate(
                //CfClient.getRequestFactory()
                ).getForEntity("https://api.dev.cfdev.sh/v2/info", ApiInfo.class);

        List<Resource<Space>> spaces = cfClient.getResources("https://api.dev.cfdev.sh/v2/spaces", Space.class);
        Assert.assertNotNull(spaces);
    }*/
}
