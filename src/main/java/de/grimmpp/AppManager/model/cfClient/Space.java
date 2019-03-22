package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

@Data
public class Space {
    private String name;
    private String organization_guid;
    private String space_quota_definition_guid;
    private Boolean allow_ssh;
    private String organization_url;
    private String developers_url;
    private String managers_url;
    private String auditors_url;
    private String apps_url;
    private String routes_url;
    private String domains_url;
    private String service_instances_url;
    private String app_events_url;
    private String events_url;
    private String security_groups_url;
    private String staging_security_groups_url;
}
