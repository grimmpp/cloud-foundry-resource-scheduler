package de.grimmpp.AppManager.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ServiceInfo {
    private String binding_name;
    private Map<String,String> credentials;
    private String instance_name;
    private String label;
    private String name;
    private String plan;
    private String provider;
    private String syslog_drain_url;
    private String[] tags;
    private String[] volume_mounts;
}
