package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class HttpEndpointSchedulerMockController extends AbstractMockController{

    @RequestMapping("/httpEndpointScheduler")
    @ResponseBody
    public String mockEndpoint(HttpServletRequest request) {
        String result = "result";
        String url = request.getRequestURL().toString();
        if (!request.getQueryString().isEmpty()) url += "?"+request.getQueryString();
        insertLastOperation(url, request.getMethod(), "", result);
        return result;
    }
}
