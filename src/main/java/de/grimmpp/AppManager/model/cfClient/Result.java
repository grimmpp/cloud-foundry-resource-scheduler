package de.grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.List;

@Data
public class Result<Entity> {

    private Integer total_results;
    private Integer total_pages;
    private String prev_url;
    private String next_url;

    private List<Resource<Entity>> resources;
}
