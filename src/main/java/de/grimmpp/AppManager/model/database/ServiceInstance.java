package de.grimmpp.AppManager.model.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import javax.persistence.*;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "service_instance")
@Table(name = "service_instance")
public class ServiceInstance {

    public ServiceInstance(CreateServiceInstanceRequest si) {
        setOrgId(si.getOrganizationGuid());
        setServiceInstanceId(si.getServiceInstanceId());
        setServicePlanId(si.getPlanId());
        setSpaceId(si.getSpaceGuid());
    }

    @Id
    @Column(unique = true)
    private String serviceInstanceId;
    private String orgId;
    private String spaceId;
    private String servicePlanId;

    @Override
    public String toString() {
        return String.format("ServiceInstanceId: %s, ", serviceInstanceId);
    }
}


