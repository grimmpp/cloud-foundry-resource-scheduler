package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@Controller()
public class CfApiMockController extends AbstractMockController{

    @Autowired
    private HttpServletRequest request;

    @RequestMapping("/cf_api_mock/v2/info")
    @ResponseBody
    public String getApiInfo() throws IOException {
        String response = getResourceContent("apiInfo");
        insertLastOperation(request, "", response);

        return response;
    }

    @RequestMapping("/cf_api_mock/oauth/token")
    @ResponseBody
    public String getMockToken() throws IOException {
        String response = getResourceContent("oauthToken");

        insertLastOperation(request,"", response);

        return response;
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

        insertLastOperation(request, "", respBody);

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
        String respBody = "";

        insertLastOperation(request, "", respBody);

        return respBody;
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

        insertLastOperation(request, "", respBody);

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

        insertLastOperation(request, reqBody, respBody);

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

        insertLastOperation(request, "", respBody);

        return respBody;
    }

}
