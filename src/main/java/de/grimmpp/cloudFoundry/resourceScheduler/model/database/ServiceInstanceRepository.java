package de.grimmpp.cloudFoundry.resourceScheduler.model.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceInstanceRepository extends CrudRepository<ServiceInstance, String> {
    ServiceInstance findByServiceInstanceId(String id);
    List<ServiceInstance> findByServicePlanId(String planId);
}
