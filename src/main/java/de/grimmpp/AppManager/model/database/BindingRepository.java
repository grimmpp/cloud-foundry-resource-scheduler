package de.grimmpp.AppManager.model.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BindingRepository extends CrudRepository<Binding, String> {
}
