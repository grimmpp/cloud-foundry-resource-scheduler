package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class Application {

    private String name;
    private Boolean production;
    private String space_guid;
    private String stack_guid;
    private String buildpack;
    private String detected_buildpack;
    private String detected_buildpack_guid;
    private Map<String,Object> environment_json;
    private Integer memory;
    private Integer instances;
    private Integer disk_quota;
    private String state;
    private String version;
    private String command;
    private Boolean console;
    private String debug;
    private String staging_task_id;
    private String package_state;
    private String health_check_http_endpoint;
    private String health_check_type;
    private String health_check_timeout;
    private String staging_failed_reason;
    private String staging_failed_description;
    private Boolean diego;
    private String docker_image;
    private Map<String,String> docker_credentials;
    private Date package_updated_at;
    private String detected_start_command;
    private Boolean enable_ssh;
    private String space_url;
    private String stack_url;
    private String routes_url;
    private String events_url;
    private String service_bindings_url;
    private String route_mappings_url;
}
