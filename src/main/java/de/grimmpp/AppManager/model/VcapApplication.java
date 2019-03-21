package de.grimmpp.AppManager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcapApplication {

    private String application_id;
    private String application_name;
    private String[] application_uris;
    private String application_version;
    private String cf_api;
    private String host;
    private String instance_id;
    private Integer instance_index;
    private Limits limits;
    private String name;
    private Integer port;
    private String space_id;
    private String space_name;
    private String[] uris;
    private String versions;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Limits {
        private Integer disk;
        private Integer fds;
        private Integer mem;
    }
}
