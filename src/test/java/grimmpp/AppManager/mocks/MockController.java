package grimmpp.AppManager.mocks;

import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MockController {

    public List<Map<String, String>> lastOperations = new ArrayList<>();

    @RequestMapping("/v2/{resources}/{id}/{dependentResources}")
    @ResponseBody
    public String getDependentResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @PathVariable("dependentResources") String dependentResources)
                throws IOException {

        lastOperations.add(0, new HashMap<String, String>(){{
            put("HttpMethod", "GET");
            put("URL", "/v2/"+resources+"/"+id+"/"+dependentResources);
        }});

        if (resources.equals("service_plans") && dependentResources.equals("service_instances")) {
            return getResourceContent("serviceInstancesByPlanId");
        }

        if (resources.equals("service_instances") && dependentResources.equals("service_bindings")) {
            return getResourceContent("bindingBySI_" + id);
        }

        if (resources.equals("spaces") && dependentResources.equals("apps")) {
            return getResourceContent("space_"+id+"_apps");
        }

        return null;
    }

    @RequestMapping("/v2/{resources}/{id}")
    @ResponseBody
    public String getParticularResource(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        lastOperations.add(0, new HashMap<String, String>(){{
            put("HttpMethod", "GET");
            put("URL", "/v2/"+resources+"/"+id);
        }});

        if (resources.equals("apps")) {
            return getResourceContent("app_"+id);
        }

        return null;
    }

    @RequestMapping(value = "/v2/{resources}/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public String updateParticularResource(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @RequestBody String body)
            throws IOException {

        lastOperations.add(0, new HashMap<String, String>(){{
            put("HttpMethod", "PUT");
            put("URL", "/v2/"+resources+"/"+id);
            put("Body", body);
        }});

        return "";
    }

    @RequestMapping("/v2/{resources}")
    @ResponseBody
    public String getResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        lastOperations.add(0, new HashMap<String, String>(){{
            put("HttpMethod", "GET");
            put("URL", "/v2/"+resources);
        }});

        if (resources.equals("apps")) {
            return getResourceContent("");
        }

        return null;
    }


    public static String getResourceContent(String resourceName) throws IOException {
        String path = "classpath:CloudControllerMock/"+resourceName+".json";

        File file = ResourceUtils.getFile(path);
        String content = new String(Files.readAllBytes(file.toPath()));

        return content;
    }
}
