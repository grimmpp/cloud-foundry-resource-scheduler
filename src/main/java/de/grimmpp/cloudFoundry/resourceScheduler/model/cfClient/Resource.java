package de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient;

import lombok.Data;

@Data
public class Resource<Entity> {
    private Metadata metadata;
    private Entity entity;
}
