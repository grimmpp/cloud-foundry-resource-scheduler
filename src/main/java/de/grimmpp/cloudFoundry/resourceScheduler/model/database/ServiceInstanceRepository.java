package de.grimmpp.cloudFoundry.resourceScheduler.model.database;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceInstanceRepository extends CrudRepository<ServiceInstance, String> {

    ServiceInstance findByServiceInstanceId(String id);

    List<ServiceInstance> findByServicePlanId(String planId);

    @Query("SELECT si FROM service_instance si WHERE si.servicePlanId = :planId AND si.id % :cCount = :index")
    List<ServiceInstance> findByServicePlanIdAndAppInstanceIndex(@Param("planId") String planId, @Param("index") long index, @Param("cCount") long containerCount);
}
