package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class HttpEndpointSchedulerMockController extends AbstractMockController{

    @Autowired
    private HttpServletRequest request;

    @RequestMapping("/httpEndpointScheduler")
    @ResponseBody
    public String mockEndpoint(HttpServletRequest request) throws IOException {
        String result = "result";
        String url = request.getRequestURL().toString();
        if (!request.getQueryString().isEmpty()) url += "?"+request.getQueryString();

        insertLastOperation(request, "", result);

        return result;
    }
}
