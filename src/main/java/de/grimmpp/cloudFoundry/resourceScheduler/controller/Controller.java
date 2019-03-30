package de.grimmpp.cloudFoundry.resourceScheduler.controller;

import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class Controller {

    @Autowired
    private ServiceInstanceRepository siRepo;

    @RequestMapping("/v2/service_instances")
    @ResponseBody
    public List<ServiceInstance> getServiceInstances() {
        List<ServiceInstance> result = new ArrayList<>();

        siRepo.findAll().forEach(result::add);

        return result;
    }
}
