package de.grimmpp.cloudFoundry.resourceScheduler.model.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.service.TimeParameterValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "parameter")
@Table(name = "parameter")
public class Parameter implements Serializable {

    private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    public static final String KEY_FIXED_DELAY = "fixedDelay";
    public static final String KEY_TIMES = "times";
    public static final String KEY_HTTP_METHOD = "httpMethod";
    public static final String KEY_HTTP_HEADERS = "httpHeaders";
    public static final String KEY_SSL_ENABLED = "sslEnabled";
    public static final String KEY_URL = "url";
    public static final String[] MANDATORY_PARAMETERS = new String[]{ KEY_FIXED_DELAY, KEY_URL};
    //public static final String[] OPTIONAL_PARAMETERS = new String[]{ KEY_HTTP_METHOD, KEY_HTTP_HEADERS };
    public static final String KEY_LAST_CALL = "lastCall";

    @Id @GeneratedValue
    private long id;
    @NotNull
    private String reference;
    @NotNull
    private String key;
    private String value;

    public Parameter(String reference, String key, String value) {
        this.reference = reference;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("Parameter: Reference: %s, Key: %s, Value: %s", reference, key, value);
    }

    public static List<Parameter> convert(CreateServiceInstanceRequest request) {
        return convert(request.getServiceInstanceId(), request.getParameters());
    }

    public static List<Parameter> convert(String reference, Map<String,Object> parameters) {
        List<Parameter> params = new ArrayList<>();
        for(String key: parameters.keySet()) {
            String value = getStringValue(parameters.get(key), "");
            params.add(new Parameter(reference, key, value));
        }
        return params;
    }

    public static void updateList(String reference, List<Parameter> parameters, Map<String,Object> map) {
        for(String key: map.keySet()) {
            boolean updated = false;
            for(Parameter p: parameters) {
                if (p.key.equals(key)) {
                    p.value = getStringValue(map.get(key), p.value);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                parameters.add(new Parameter(reference, key, getStringValue(map.get(key), "")));
            }
        }
    }

    private static String getStringValue(Object o, String defaultValue) {
        if (o != null && o instanceof String) return o.toString();
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert parameter value", e);
        }
        return defaultValue;
    }

    public static Parameter getParameterByKey(List<Parameter> params, String key) {
        return params.stream().filter(p -> p.getKey().equals(key)).findFirst().get();
    }

    public static String getParameterValueByKey(List<Parameter> params, String key) {
        return getParameterByKey(params, key).getValue();
    }

    public static boolean containsKey(List<Parameter> params, String key) {
        return params.stream().anyMatch(p -> p.getKey().equals(key));
    }
}
