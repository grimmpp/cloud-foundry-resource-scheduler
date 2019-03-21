package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.Date;

@Data
public class AppInstanceState {
    private String state;
    private Date since;
    private Long uptime;
}
