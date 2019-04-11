package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.config.AppConfig;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.service.CfClient;
import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMockController {

    protected static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Autowired
    private VcapApplication vcapApp;

    public static final String BASE_URL = "http://localhost:8111/cf_api_mock";
    public static final String KEY_URL = "URL";
    public static final String KEY_REQUEST_BODY = "ReqBody";
    public static final String KEY_RESPONSE_BODY = "RespBody";
    public static final String KEY_HTTP_METHOD = "HttpMethod";
    public final List<Map<String, String>> lastOperations = new ArrayList<>();

    public String getLastOperation(String key) {
        return lastOperations.get(0).get(key);
    }

    public void insertLastOperation(HttpServletRequest request, String requestBody, String respBody) throws IOException {
        lastOperations.add(0, new HashMap<String, String>(){{
            String url = request.getRequestURL().toString();
            if (request.getQueryString() != null) url += "?"+request.getQueryString();
            //url = url.replace("http://localhost:8111/cf_api_mock", "");

            put(KEY_URL, url);
            //request body stream was already used from spring boot and is empty now. ...
            put(KEY_REQUEST_BODY, requestBody);
            put(KEY_HTTP_METHOD, request.getMethod());
            put(KEY_RESPONSE_BODY, respBody);

            String value = request.getHeader(AppConfig.HEADER_NAME_CF_SENDER_APP);
            put(AppConfig.HEADER_NAME_CF_SENDER_APP, value);

        }});
    }

    public static String getResourceContent(String resourceName) throws IOException {
        String path = "classpath:CloudControllerMock/"+resourceName+".json";

        File file = ResourceUtils.getFile(path);
        String content = new String(Files.readAllBytes(file.toPath()));

        return content;
    }

    public static <ReturnType> ReturnType getResourceContent(String resourceName, Class<ReturnType> returnType) throws IOException {
        String content = getResourceContent(resourceName);
        return (ReturnType) objectMapper.readValue(content, returnType);
    }
}
