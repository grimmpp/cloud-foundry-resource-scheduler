package grimmpp.AppManager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import grimmpp.AppManager.model.database.ServiceInstance;
import grimmpp.AppManager.model.database.ServiceInstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

/**
 * See docs
 * Service Instances: https://docs.spring.io/spring-cloud-open-service-broker/docs/current/reference/html5/#example-implementation
 * Bindings: https://docs.spring.io/spring-cloud-open-service-broker/docs/current/reference/html5/#example-implementation-2
 */

@Slf4j
@Service
public class BrokerController implements ServiceInstanceService, ServiceInstanceBindingService {

    @Autowired
    private ServiceInstanceRepository siRepo;

    ObjectMapper objectMapper = new ObjectMapper();

    private void logRequest(Object request) {
        try {
            log.info("CreateServiceInstanceRequest: "+objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        logRequest(request);


        String time = "8h";
        if (request.getParameters().containsKey("time")) time = request.getParameters().get("time").toString();

        ServiceInstance si = new ServiceInstance();
        si.setServiceInstanceId(request.getServiceInstanceId());
        si.setServicePlanId(request.getPlanId());
        si.setPropertyTime(time);
        si.setSpaceId(request.getSpaceGuid());
        si.setOrgId(request.getOrganizationGuid());
        siRepo.save(si);

        return CreateServiceInstanceResponse.builder()
                .async(false)
                .dashboardUrl(time)
                .build();
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
        logRequest(request);
        // not needed
        return null;
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
        logRequest(request);
        return DeleteServiceInstanceResponse.builder().build();
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        logRequest(request);
        // not needed
        return null;
    }

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        logRequest(request);

        String time = "8d";
        if (request.getParameters().containsKey("time")) time = request.getParameters().get("time").toString();

        return CreateServiceInstanceAppBindingResponse.builder()
                .bindingExisted(false)
                .credentials("time", time)
                .build();
    }

    @Override
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        logRequest(request);
    }
}
