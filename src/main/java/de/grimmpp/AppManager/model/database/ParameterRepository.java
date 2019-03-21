package de.grimmpp.AppManager.model.database;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterRepository extends CrudRepository<Parameter, String> {
    //@Query(value = "SELECT p.* FROM parameter p where p.reference = (:reference)", nativeQuery = true)
    List<Parameter> findByReference(String reference);

    Parameter findByReferenceAndKey(String reference, String key);
}
