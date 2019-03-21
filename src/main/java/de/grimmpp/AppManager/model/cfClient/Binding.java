package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.Map;

@Data
public class Binding {
    private String app_guid;
    private String service_instance_guid;
    private Map<String,String> credentials;
    private Map<String,String> binding_options;
    private String gateway_data;
    private String gateway_name;
    private String syslog_drain_url;
    // volume_mounts
    private String name;
    private LastOperation last_operation;
    private String app_url;
    private String service_instance_url;
    private String service_binding_parameters_url;
}
