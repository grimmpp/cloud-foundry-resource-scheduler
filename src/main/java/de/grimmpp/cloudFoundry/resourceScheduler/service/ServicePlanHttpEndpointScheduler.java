package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ServicePlanHttpEndpointScheduler extends IServicePlanBasedOnServiceInstance {

    public static final String PLAN_ID = "d0704f41-4a2e-4bea-b1f7-2319640cbe97";
    public static final String PARAMETER_KEY_HTTP_METHOD = "httpMethod";
    public static final String PARAMETER_KEY_HTTP_HEADERS = "httpHeaders";
    public static final String PARAMETER_KEY_URL = "url";
    public static final String[] MANDATORY_PARAMETERS = new String[]{ TimeParameterValidator.KEY, PARAMETER_KEY_URL};
    public static final String[] OPTIONAL_PARAMETERS = new String[]{ PARAMETER_KEY_HTTP_METHOD, PARAMETER_KEY_HTTP_HEADERS };
    public static final String PARAMETER_KEY_LAST_CALL = "lastCall";

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    private RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(1))
            .setReadTimeout(Duration.ofSeconds(1))
            .build();

    @Override
    protected void performActionForServiceInstance(ServiceInstance si) throws IOException {
        log.debug("Check service instance {} for doing http call.", si.getServiceInstanceId());

        List<Parameter> params = pRepo.findByReference(si.getServiceInstanceId());

        if (isTimeExpired(params)) {
            String time = params.stream().filter(p -> p.getKey().equals(TimeParameterValidator.KEY)).findFirst().get().getValue();
            log.debug("time is expired after {} milli sec.", time);
            try {
                String url = params.stream().filter(p -> p.getKey().equals(PARAMETER_KEY_URL)).findFirst().get().getValue();
                String httpMethod = params.stream().filter(p -> p.getKey().equals(PARAMETER_KEY_HTTP_METHOD)).findFirst().get().getValue();
                HttpHeaders headers = new HttpHeaders();
                String headersStr = params.stream().filter(p -> p.getKey().equals(PARAMETER_KEY_HTTP_HEADERS)).findFirst().get().getValue();
                for (String header: objectMapper.readValue(headersStr, String[].class)) {
                    headers.set(header.split(": ")[0], header.split(": ")[1]);
                }
                HttpEntity<String> entity = new HttpEntity<>(headers);

                log.debug("Do {} call to url {} - ", httpMethod, url);
                restTemplate.exchange(url, HttpMethod.valueOf(httpMethod), entity, String.class);

                //TODO: lastCall parameter must be set & it's not yet tested
            } catch (Throwable e) {
                log.error("Was not able to do http call. ", e);
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
                .key(PARAMETER_KEY_URL)
                .value(request.getParameters().get(PARAMETER_KEY_URL).toString())
                .build());

        // time
        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        params.add(Parameter.builder()
                .reference(request.getServiceInstanceId())
                .key(TimeParameterValidator.KEY)
                .value(request.getParameters().get(TimeParameterValidator.KEY).toString())
                .build());

        // http method
        if (request.getParameters().containsKey(PARAMETER_KEY_HTTP_METHOD)) {
            params.add(Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(PARAMETER_KEY_HTTP_METHOD)
                    .value(request.getParameters().get(PARAMETER_KEY_HTTP_METHOD).toString())
                    .build());
        }

        // http headers
        if (request.getParameters().containsKey(PARAMETER_KEY_HTTP_HEADERS)) {
            try {
                String headers = objectMapper.writeValueAsString(request.getParameters().get(PARAMETER_KEY_HTTP_HEADERS));
                params.add(Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(PARAMETER_KEY_HTTP_HEADERS)
                        .value(headers)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        params.add(Parameter.builder()
            .reference(request.getServiceInstanceId())
            .key(PARAMETER_KEY_LAST_CALL)
            .value("0")
            .build());

        pRepo.saveAll(params);
    }

    private void checkMandatoryParams(CreateServiceInstanceRequest request) {
        for (String mp: MANDATORY_PARAMETERS) {
            if (!request.getParameters().keySet().contains(mp)) {
                throw new RuntimeException(
                        String.format("Request for service instance %s does not contain parameter %s",
                                request.getServiceInstanceId(), mp));
            }
        }
    }

    private void validateUrl(CreateServiceInstanceRequest request) {
        String urlValue = request.getParameters().get(PARAMETER_KEY_URL).toString();
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
        String value = request.getParameters().get(TimeParameterValidator.KEY).toString();
        TimeParameterValidator.validateParameterValue(value);
    }

    private void validateHttpMethod(CreateServiceInstanceRequest request){
        if (request.getParameters().containsKey(PARAMETER_KEY_HTTP_METHOD)) {
            String value = request.getParameters().get(PARAMETER_KEY_HTTP_METHOD).toString();
            if (!Arrays.asList(HttpMethod.values()).stream().anyMatch(v -> v.toString().equals(value))) {
                throw new RuntimeException(String.format("%s is no supported http method", value));
            }
        }
    }

    private void validateHttpHeaders(CreateServiceInstanceRequest request){
        if (request.getParameters().containsKey(PARAMETER_KEY_HTTP_HEADERS)) {
            Object paramValues = request.getParameters().get(PARAMETER_KEY_HTTP_HEADERS);
            if (!(paramValues instanceof String[])) {
                throw new RuntimeException(
                        String.format("Optional Parameter %s does not support type %s as value.",
                                PARAMETER_KEY_HTTP_HEADERS, paramValues.getClass().getSimpleName()));
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
        Parameter pLastCall = params.stream().filter(p -> p.getKey().equals(PARAMETER_KEY_LAST_CALL)).findFirst().get();
        long lastCallTime = Long.valueOf(pLastCall.getValue());
        Parameter pTimeSpan = params.stream().filter(p -> p.getKey().equals(TimeParameterValidator.KEY)).findFirst().get();
        long timeSpan = TimeParameterValidator.getTimeInMilliSecFromParameterValue(pTimeSpan.getValue());
        long currentTime = System.currentTimeMillis();
        log.debug("lastCallTime: {}, timeSpan: {}, currentTime: {}", lastCallTime, timeSpan, currentTime);
        return (System.currentTimeMillis() - lastCallTime) > timeSpan;
    }
}
