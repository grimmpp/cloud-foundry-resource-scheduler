package grimmpp.AppManager.model.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceInstanceRepository extends CrudRepository<ServiceInstance, String> {
    ServiceInstance findByServiceInstanceId(String id);
}
