package grimmpp.AppManager.mocks;

import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Controller
public class MockController {

    @RequestMapping("/v2/{resources}/{id}/{dependentResources}")
    @ResponseBody
    public String getDependentResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id,
            @PathVariable("dependentResources") String dependentResources)
                throws IOException {

        if (resources.equals("service_plans") && dependentResources.equals("service_instances")) {
            return getResourceContent("serviceInstancesByPlanId");
        }

        if (resources.equals("service_instances") && dependentResources.equals("service_bindings")) {
            return getResourceContent("bindingBySI_" + id);
        }

        return null;
    }

    @RequestMapping("/v2/{resources}/{id}")
    @ResponseBody
    public String getParticularResource(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        if (resources.equals("apps")) {
            return getResourceContent("app_"+id);
        }

        return null;
    }

    @RequestMapping("/v2/{resources}")
    @ResponseBody
    public String getResources(
            @PathVariable("resources") String resources,
            @PathVariable("id") String id)
            throws IOException {

        return null;
    }


    public static String getResourceContent(String resourceName) throws IOException {
        String path = "classpath:CloudControllerMock/"+resourceName+".json";

        File file = ResourceUtils.getFile(path);
        String content = new String(Files.readAllBytes(file.toPath()));

        return content;
    }
}
