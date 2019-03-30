package de.grimmpp.cloudFoundry.resourceScheduler.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ServicePlanFinder;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.logging.LogLevel;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private ParameterRepository paraRepo;

    @Autowired
    BindingRepository bindingRepo;

    @Autowired
    @Qualifier("ProjectLogLevel")
    private String projectLogLevel;

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    private void logRequest(Object request) {
        if (projectLogLevel.toLowerCase().equals(LogLevel.DEBUG.toString().toLowerCase()) ||
            projectLogLevel.toLowerCase().equals(LogLevel.TRACE.toString().toLowerCase())) {
            try {
                log.info("CreateServiceInstanceRequest: " + objectMapper.writeValueAsString(request));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
        logRequest(request);

        siRepo.save(new ServiceInstance(request));
        ServicePlanFinder.findServicePlan(request.getPlanId()).saveRequestParamters(request);

        return CreateServiceInstanceResponse.builder()
                .async(false)
                .build();
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
        logRequest(request);
        // not needed
        return null;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
        logRequest(request);

        paraRepo.deleteAll(paraRepo.findByReference(request.getServiceInstanceId()));

        ServiceInstance si = siRepo.findByServiceInstanceId(request.getServiceInstanceId());
        if (si != null) siRepo.delete(si);

        return DeleteServiceInstanceResponse.builder().build();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
        logRequest(request);

        ServiceInstance si = siRepo.findByServiceInstanceId(request.getServiceInstanceId());
        if (si != null) {
            List<Parameter> parameters = paraRepo.findByReference(si.getServiceInstanceId());
            Parameter.updateList(si.getServiceInstanceId(), parameters, request.getParameters());
            paraRepo.saveAll(parameters);
        }

        return UpdateServiceInstanceResponse.builder()
                .async(false)
                .build();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        logRequest(request);

        ServicePlanFinder.findServicePlan(request.getPlanId()).saveRequestParamters(request);

        Binding sb = new Binding(request);
        bindingRepo.save(sb);
        paraRepo.saveAll(Parameter.convert(sb.getBindingId(), request.getParameters()));

        return CreateServiceInstanceAppBindingResponse.builder()
                .bindingExisted(false)
                .credentials(request.getParameters())
                .build();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        logRequest(request);

        List<Parameter> parameters = paraRepo.findByReference(request.getBindingId());
        paraRepo.deleteAll(parameters);

        Optional<Binding> b = bindingRepo.findById(request.getBindingId());
        if (b.isPresent()) bindingRepo.delete(b.get());

        logRequest(request);
    }
}
