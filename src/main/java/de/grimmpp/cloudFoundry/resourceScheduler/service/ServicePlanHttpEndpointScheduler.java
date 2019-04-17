package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
public class ServicePlanHttpEndpointScheduler extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "d0704f41-4a2e-4bea-b1f7-2319640cbe97";

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
    private RestTemplate restTemplate = null;
    private RestTemplate noSslRestTempalte = null;

    @PostConstruct
    public void createRestTemplates() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        createNoSslRestTemplate();
        createRestTemplate();
    }

    private void createNoSslRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (noSslRestTempalte == null) {
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .build();

            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory();

            requestFactory.setHttpClient(httpClient);
            requestFactory.setConnectTimeout(500);
            requestFactory.setReadTimeout(500);

            noSslRestTempalte = new RestTemplate(requestFactory);
        }
    }

    private void createRestTemplate() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(500);
            requestFactory.setReadTimeout(500);
            restTemplate = new RestTemplate(requestFactory);
        }
    }

    private RestTemplate getRestTemplate(boolean sslEnabled) {
        if (!sslEnabled) {
            log.debug("SSL is disabled for this call!");
            return noSslRestTempalte;

        } else {
            return restTemplate;
        }
    }

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {
        log.debug("Check service instance {} for doing http call.", si.getServiceInstanceId());

        List<Parameter> params = pRepo.findByReference(si.getServiceInstanceId());

        if (isTimeExpired(params)) {
            String timeKey = TimeParameterValidator.getContainedTimeParameter(params);
            String time = Parameter.getParameterValueByKey(params, timeKey);
            log.debug("Time is expired after -> mode: '{}' parameter: {}.", timeKey, time);

            HttpEntity<String> entity = new HttpEntity<>(getHeaders(params));
            String url = Parameter.getParameterValueByKey(params, Parameter.KEY_URL);
            String httpMethod = Parameter.getParameterValueByKey(params, Parameter.KEY_HTTP_METHOD);
            Boolean sslEnabled = Boolean.valueOf(Parameter.getParameterValueByKey(params, Parameter.KEY_SSL_ENABLED));

            log.debug("Do {} call to url {}", httpMethod, url);
            getRestTemplate(sslEnabled).exchange(url, HttpMethod.valueOf(httpMethod), entity, String.class);

            // Remember last http call made.
            Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL).setValue(Long.toString(System.currentTimeMillis()));
            pRepo.save(Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL));
            log.info("=> Instance {} called via {} {} ", si.getServiceInstanceId(), httpMethod, url);

        } else {
            log.debug("time is not expired.");
        }
    }

    private HttpHeaders getHeaders(List<Parameter> parameters) throws IOException {

        String parameterHeadersAsStr = Parameter.getParameterValueByKey(parameters, Parameter.KEY_HTTP_HEADERS);

        HttpHeaders headers = new HttpHeaders();

        for (String header : objectMapper.readValue(parameterHeadersAsStr, String[].class)) {
            headers.set(header.split(": ")[0], header.split(": ")[1]);
        }
        headers.add(AppConfig.HEADER_NAME_CF_SENDER_APP, appConfig.getSenderAppHttpHeaderValue());

        return headers;
    }

    @Override
    public String getServicePlanId() {
        return PLAN_ID;
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceBindingRequest request) {
        // Not in use.
    }

    @Override
    public void saveRequestParamters(CreateServiceInstanceRequest request) {
        validateUrl(request);
        if (!TimeParameterValidator.validateParameterValue(request.getParameters())) {
            throw new RuntimeException("Invalid time parameters.");
        }

        validateHttpMethod(request);
        validateHttpHeaders(request);

        List<Parameter> params = new ArrayList<>();
        // url
        params.add(Parameter.builder()
                .reference(request.getServiceInstanceId())
                .key(Parameter.KEY_URL)
                .value(request.getParameters().get(Parameter.KEY_URL).toString())
                .build());

        // time
        params.add(TimeParameterValidator.getTimeParameter(request));

        // http method
        if (request.getParameters().containsKey(Parameter.KEY_HTTP_METHOD)) {
            params.add(Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(Parameter.KEY_HTTP_METHOD)
                    .value(request.getParameters().get(Parameter.KEY_HTTP_METHOD).toString())
                    .build());
        } else {
            params.add(Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(Parameter.KEY_HTTP_METHOD)
                    .value(HttpMethod.GET.toString())
                    .build());
        }

        // http headers
        if (request.getParameters().containsKey(Parameter.KEY_HTTP_HEADERS)) {
            try {
                String headers = objectMapper.writeValueAsString(request.getParameters().get(Parameter.KEY_HTTP_HEADERS));
                params.add(Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(Parameter.KEY_HTTP_HEADERS)
                        .value(headers)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                String headers = objectMapper.writeValueAsString(new String[]{});
                params.add(Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(Parameter.KEY_HTTP_HEADERS)
                        .value(headers)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        // ssl enabled
        if (request.getParameters().containsKey(Parameter.KEY_SSL_ENABLED)) {
            params.add(Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(Parameter.KEY_SSL_ENABLED)
                    .value(request.getParameters().get(Parameter.KEY_SSL_ENABLED).toString())
                    .build());
        } else {
            params.add(Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(Parameter.KEY_SSL_ENABLED)
                    .value(Boolean.TRUE.toString())
                    .build());
        }

        params.add(Parameter.builder()
            .reference(request.getServiceInstanceId())
            .key(Parameter.KEY_LAST_CALL)
            .value("0")
            .build());

        pRepo.saveAll(params);
    }

    private void validateUrl(CreateServiceInstanceRequest request) {
        if (!request.getParameters().containsKey(Parameter.KEY_URL)) {
            throw new RuntimeException("Mandatory Parameter url is not contained.");
        }

        String urlValue = request.getParameters().get(Parameter.KEY_URL).toString();
        try {
            new URL(urlValue);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        if (!(urlValue.startsWith("http://") || urlValue.startsWith("https://"))) {
            throw new RuntimeException("Parameter url must start either with 'http://' or 'https://'");
        }
    }

    private void validateHttpMethod(CreateServiceInstanceRequest request){
        if (request.getParameters().containsKey(Parameter.KEY_HTTP_METHOD)) {
            String value = request.getParameters().get(Parameter.KEY_HTTP_METHOD).toString();
            if (!Arrays.asList(HttpMethod.values()).stream().anyMatch(v -> v.toString().equals(value))) {
                throw new RuntimeException(String.format("%s is no supported http method", value));
            }
        }
    }

    private void validateHttpHeaders(CreateServiceInstanceRequest request){
        if (request.getParameters().containsKey(Parameter.KEY_HTTP_HEADERS)) {
            Object paramValues = request.getParameters().get(Parameter.KEY_HTTP_HEADERS);
            if (!(paramValues instanceof String[])) {
                throw new RuntimeException(
                        String.format("Optional Parameter %s does not support type %s as value.",
                                Parameter.KEY_HTTP_HEADERS, paramValues.getClass().getSimpleName()));
            }

            for(String header: (String[])paramValues) {
                if (!header.contains(": ") ||
                    header.split(": ").length != 2 ||
                    header.split(": ")[0].length() < 2 ||
                    header.split(": ")[1].length() < 2 ) {

                    throw new RuntimeException(
                            String.format("Format of http header %s is not supported.", header ));
                }
            }
        }
    }

    private boolean isTimeExpired(List<Parameter> params) throws IOException {
        boolean isExpired = false;
        String key = TimeParameterValidator.getContainedTimeParameter(params);

        long lastCallTime = Long.valueOf(Parameter.getParameterValueByKey(params, Parameter.KEY_LAST_CALL));
        long currentTime = System.currentTimeMillis();

        if (Parameter.KEY_FIXED_DELAY.equals(key)) {
            long fixedDelayInMilliSec = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue(params);
            isExpired = (currentTime - lastCallTime) > fixedDelayInMilliSec;
            log.debug("IsExpired {}, fixedDelay: {} milli sec, lastCall: {} milli sec, currentTime: {} milli sec",
                    isExpired, fixedDelayInMilliSec, lastCallTime, currentTime);

        } else if (Parameter.KEY_TIMES.equals(key)) {
            isExpired = TimeParameterValidator.isTimesExpired(params);
        }

        return isExpired;
    }
}
