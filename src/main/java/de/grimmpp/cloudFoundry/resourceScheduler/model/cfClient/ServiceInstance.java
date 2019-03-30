package de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient;

import lombok.Data;

import java.util.Map;

@Data
public class ServiceInstance {

    private String name;
    private Map<String,String> credentials;
    private String service_plan_guid;
    private String space_guid;
    private String gateway_data;
    private String dashboard_url;
    private String type;
    private String last_operation;
    private String[] tags;
    private String space_url;
    private String service_plan_url;
    private String service_bindings_url;
    private String service_keys_url;
    private String routes_url;
    private String shared_from_url;
    private String shared_to_url;
    private String service_instance_parameters_url;
}
