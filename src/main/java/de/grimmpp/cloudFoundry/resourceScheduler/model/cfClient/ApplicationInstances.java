package de.grimmpp.cloudFoundry.resourceScheduler.model.cfClient;

import lombok.Data;

import java.util.HashMap;

@Data
public class ApplicationInstances extends HashMap<String, AppInstanceState> {
}
