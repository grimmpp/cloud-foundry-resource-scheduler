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
public class CfApiMockController extends AbstractMockController{

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

}
