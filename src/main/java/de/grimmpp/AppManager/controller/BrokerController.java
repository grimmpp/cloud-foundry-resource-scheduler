package de.grimmpp.AppManager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.model.database.*;
import de.grimmpp.AppManager.service.TimeParameterValidator;
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

    @Autowired BindingRepository bindingRepo;

    @Autowired
    @Qualifier("ProjectLogLevel")
    private String projectLogLevel;

    private ObjectMapper objectMapper = new ObjectMapper();

    private void logRequest(Object request) {
        if (projectLogLevel.equals(LogLevel.DEBUG) ||
            projectLogLevel.equals(LogLevel.TRACE)) {
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
        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        paraRepo.save(
                Parameter.builder()
                    .reference(request.getServiceInstanceId())
                    .key(TimeParameterValidator.KEY)
                    .value(time)
                    .build());

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
        siRepo.delete(siRepo.findByServiceInstanceId(request.getServiceInstanceId()));

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

        String time = TimeParameterValidator.getParameterTime(request, TimeParameterValidator.DEFAULT_VALUE);
        paraRepo.save(
                Parameter.builder()
                        .reference(request.getServiceInstanceId())
                        .key(TimeParameterValidator.KEY)
                        .value(time)
                        .build());

        Binding sb = new Binding(request);
        bindingRepo.save(sb);
        paraRepo.saveAll(Parameter.convert(sb.getBindingId(), request.getParameters()));

        return CreateServiceInstanceAppBindingResponse.builder()
                .bindingExisted(false)
                .credentials("time", time)
                .build();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        logRequest(request);

        List<Parameter> parameters = paraRepo.findByReference(request.getBindingId());
        paraRepo.deleteAll(parameters);

        Binding b = bindingRepo.findById(request.getBindingId()).get();
        bindingRepo.delete(b);

        logRequest(request);
    }
}
