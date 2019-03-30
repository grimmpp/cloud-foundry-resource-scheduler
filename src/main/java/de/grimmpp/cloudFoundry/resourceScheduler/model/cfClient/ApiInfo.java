package de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiInfo {

    private String name;
    private String build;
    private String support;
    private Integer version;
    private String description;
    private String authorization_endpoint;
    private String token_endpoint;
    private String min_cli_version;
    private String min_recommended_cli_version;
    private String app_ssh_endpoint;
    private String app_ssh_host_key_fingerprint;
    private String app_ssh_oauth_client;
    private String doppler_logging_endpoint;
    private String api_version;
    private String osbapi_version;
    private String routing_endpoint;
    private String user;
}
