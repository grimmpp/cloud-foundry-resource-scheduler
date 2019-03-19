package grimmpp.AppManager.model.database;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "ServiceInstance")
public class ServiceInstance {

    @Id
    private String serviceInstanceId;
    private String orgId;
    private String spaceId;
    private String servicePlanId;
    private String propertyTime;
}
