package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
@Slf4j
public class ServicePlanHttpEndpointScheduler extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "d0704f41-4a2e-4bea-b1f7-2319640cbe97";

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
    private RestTemplate restTemplate = null;
    private RestTemplate noSslRestTempalte = null;

    private RestTemplate getRestTemplate(boolean sslEnabled) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (!sslEnabled) {
            log.debug("Disabled SSL.");
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
            return noSslRestTempalte;

        } else {
            if (restTemplate == null) {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(500);
                requestFactory.setReadTimeout(500);
                restTemplate = new RestTemplate(requestFactory);
            }
            return restTemplate;
        }
    }

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {
        log.debug("Check service instance {} for doing http call.", si.getServiceInstanceId());

        List<Parameter> params = pRepo.findByReference(si.getServiceInstanceId());

        if (isTimeExpired(params)) {
            String time = Parameter.getParameterValueByKey(params, Parameter.KEY_FIXED_DELAY);
            log.debug("time is expired after {} milli sec.", time);
            try {
                HttpHeaders headers = new HttpHeaders();
                String headersStr = Parameter.getParameterValueByKey(params, Parameter.KEY_HTTP_HEADERS);
                for (String header : objectMapper.readValue(headersStr, String[].class)) {
                    headers.set(header.split(": ")[0], header.split(": ")[1]);
                }
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = Parameter.getParameterValueByKey(params, Parameter.KEY_URL);
                String httpMethod = Parameter.getParameterValueByKey(params, Parameter.KEY_HTTP_METHOD);
                Boolean sslEnabled = Boolean.valueOf(Parameter.getParameterValueByKey(params, Parameter.KEY_SSL_ENABLED));
                log.debug("Do {} call to url {}", httpMethod, url);
                getRestTemplate(sslEnabled).exchange(url, HttpMethod.valueOf(httpMethod), entity, String.class);

                // Remember last http call made.
                Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL).setValue(Long.toString(System.currentTimeMillis()));
                pRepo.save(Parameter.getParameterByKey(params, Parameter.KEY_LAST_CALL));
            } catch (Throwable e) {
                log.error("Was not able to do http call.", e);
            }
        } else {
            log.debug("time is not expired.");
        }
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
        checkMandatoryParams(request);
        validateUrl(request);
        validateTime(request);

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
        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        params.add(Parameter.builder()
                .reference(request.getServiceInstanceId())
                .key(Parameter.KEY_FIXED_DELAY)
                .value(request.getParameters().get(Parameter.KEY_FIXED_DELAY).toString())
                .build());

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

    private void checkMandatoryParams(CreateServiceInstanceRequest request) {
        for (String mp: Parameter.MANDATORY_PARAMETERS) {
            if (!request.getParameters().keySet().contains(mp)) {
                throw new RuntimeException(
                        String.format("Request for service instance %s does not contain parameter %s",
                                request.getServiceInstanceId(), mp));
            }
        }
    }

    private void validateUrl(CreateServiceInstanceRequest request) {
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

    private void validateTime(CreateServiceInstanceRequest request){
        TimeParameterValidator.validateParameterValue(request.getParameters());
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

    private boolean isTimeExpired(List<Parameter> params) {
        Parameter pLastCall = params.stream().filter(p -> p.getKey().equals(Parameter.KEY_LAST_CALL)).findFirst().get();
        long lastCallTime = Long.valueOf(pLastCall.getValue());
        Parameter pTimeSpan = params.stream().filter(p -> p.getKey().equals(Parameter.KEY_FIXED_DELAY)).findFirst().get();
        long timeSpan = TimeParameterValidator.getFixedDelayInMilliSecFromParameterValue(pTimeSpan.getValue());
        long currentTime = System.currentTimeMillis();
        log.debug("lastCallTime: {}, timeSpan: {}, currentTime: {}", lastCallTime, timeSpan, currentTime);
        return (System.currentTimeMillis() - lastCallTime) > timeSpan;
    }
}
