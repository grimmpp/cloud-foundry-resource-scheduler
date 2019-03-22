package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

@Data
public class AppInstanceState {
    private String state;
    private Double since;
    private Long uptime;
}
