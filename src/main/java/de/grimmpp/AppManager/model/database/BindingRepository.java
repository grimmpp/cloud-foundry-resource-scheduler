package de.grimmpp.AppManager.model.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BindingRepository extends CrudRepository<Binding, String> {
    List<Binding> findByServiceInstanceId(String siId);
}
