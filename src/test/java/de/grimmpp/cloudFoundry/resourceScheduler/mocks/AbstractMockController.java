package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

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

    public static final String KEY_URL = "URL";
    public static final String KEY_REQUEST_BODY = "ReqBody";
    public static final String KEY_RESPONSE_BODY = "RespBody";
    public static final String KEY_HTTP_METHOD = "HttpMethod";
    public final List<Map<String, String>> lastOperations = new ArrayList<>();

    public String getLastOperation(String key) {
        return lastOperations.get(0).get(key);
    }

    public void insertLastOperation(String url, String httpMethod, String reqBody, String respBody) {
        lastOperations.add(0, new HashMap<String, String>(){{
            put(KEY_URL, url);
            put(KEY_REQUEST_BODY, reqBody);
            put(KEY_RESPONSE_BODY, respBody);
            put(KEY_HTTP_METHOD, httpMethod);
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
