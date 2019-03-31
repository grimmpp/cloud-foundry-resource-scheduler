package de.grimmpp.cloudFoundry.resourceScheduler.mocks;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HttpEndpointSchedulerController {

    @RequestMapping("/httpEndpointScheduler")
    @ResponseBody
    public String mockEndpoint() {
        return "result";
    }
}
