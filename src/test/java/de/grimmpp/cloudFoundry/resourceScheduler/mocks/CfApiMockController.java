package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Controller()
public class CfApiMockController {

    private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

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

    @RequestMapping("/cf_api_mock/v2/info")
    @ResponseBody
    public String getApiInfo() throws IOException {
        return getResourceContent("apiInfo");
    }

    @RequestMapping("/cf_api_mock/oauth/token")
    @ResponseBody
    public String getMockToken() throws IOException {
        return getResourceContent("oauthToken");
    }

    @RequestMapping("/cf_api_mock/v2/{resources}/{id}/{dependentResources}")
    @ResponseBody
    public String getDependentResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @PathVariable("dependentResources") String dependentResources)
                throws IOException {

        String respBody = "";

        if (resources.equals("service_plans") && dependentResources.equals("service_instances")) {
            respBody = getResourceContent("serviceInstancesByPlanId");
        }

        else if (resources.equals("service_instances") && dependentResources.equals("service_bindings")) {
            respBody = getResourceContent("bindingBySI_" + id);
        }

        else if (resources.equals("spaces") && dependentResources.equals("apps")) {
            respBody = getResourceContent("space_"+id+"_apps");
        }
        else if (resources.equals("apps") && dependentResources.equals("instances")) {
            respBody = getResourceContent("ais_"+id);
        }

        insertLastOperation("/v2/"+resources+"/"+id+"/"+dependentResources, "GET", "", respBody);

        return respBody;
    }

    @RequestMapping(value = "/cf_api_mock/v2/{resources}/{id}/{dependentResources}/{drid}", method = RequestMethod.DELETE)
    @ResponseBody
    public String delete(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @PathVariable("dependentResources") String dependentResources,
            @PathVariable("drid") String dependentResourceId)
            throws IOException {

        insertLastOperation("/v2/"+resources+"/"+id+"/"+dependentResources+"/"+dependentResourceId, "DELETE", "", "");

        return "";
    }

    @RequestMapping("/cf_api_mock/v2/{resources}/{id}")
    @ResponseBody
    public String getParticularResource(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        String respBody = "";

        if (resources.equals("apps")) {
            respBody = getResourceContent("app_"+id);
        }
        else if (resources.equals("spaces")) {
            respBody = getResourceContent("space_"+id);
        }

        insertLastOperation("/v2/"+resources+"/"+id, "GET", "", respBody);

        return respBody;
    }

    @RequestMapping(value = "/cf_api_mock/v2/{resources}/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public String updateParticularResource(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @RequestBody String reqBody)
            throws IOException {

        String respBody = "";

        insertLastOperation("/v2/"+resources+"/"+id, "PUT", reqBody, respBody);

        return respBody;
    }

    @RequestMapping("/cf_api_mock/v2/{resources}")
    @ResponseBody
    public String getResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        String respBody = "";

        if (resources.equals("apps")) {
            respBody = getResourceContent("");
        }

        insertLastOperation("/v2/"+resources, "GET", "", respBody);

        return respBody;
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