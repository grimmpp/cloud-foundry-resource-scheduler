package de.grimmpp.AppManager.model.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "service_binding")
@Table(name = "service_binding")
public class Binding {

    @Id
    @Column(unique = true)
    private String bindingId;
    private String serviceInstanceId;
    private String applicationId;

    public Binding(CreateServiceInstanceBindingRequest request){
        setBindingId(request.getBindingId());
        setServiceInstanceId(request.getServiceInstanceId());
        setApplicationId(request.getBindResource().getAppGuid());
    }

    @Override
    public String toString() {
        return String.format("ServiceBindingId: %s, ", bindingId);
    }
}
