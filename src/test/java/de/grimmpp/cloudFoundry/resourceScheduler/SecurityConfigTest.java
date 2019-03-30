package de.grimmpp.cloudFoundry.resourceScheduler;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SecurityConfigTest {

    @Value("${broker.api.admin-user.username}")
    private String adminUsername;

    @Value("${broker.api.admin-user.password}")
    private String adminPassword;

    @Test
    public void checkBasicAuthGoodCaseTest() {
        String serviceId = "385fcdd0-f9f1-42c9-929a-93981441b0b1";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = new RestTemplate().exchange(
                "http://localhost:8111/v2/catalog", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        Assert.assertTrue(resp.getStatusCode().is2xxSuccessful());
        Assert.assertTrue(resp.getBody().contains(serviceId));
    }

    @Test
    public void checkBasicAuthBadCaseTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, "wrong password 123***");
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) { return false; }
            @Override
            public void handleError(ClientHttpResponse response) { }
        });

        ResponseEntity<String> resp = restTemplate.exchange(
                "http://localhost:8111/v2/catalog", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        Assert.assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void checkIfMockIsReachableTest() {
        ResponseEntity<String> resp = new RestTemplate().getForEntity("http://localhost:8111/cf_api_mock/v2/info", String.class);
        Assert.assertTrue(resp.getStatusCode().is2xxSuccessful());
        Assert.assertTrue(resp.getBody().contains("\"authorization_endpoint\": \"http://localhost:8111/cf_api_mock\","));
    }
}
