package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.Date;

@Data
public class Metadata {

    private String guid;
    private String url;
    private Date created_at;
    private Date updated_at;
}
