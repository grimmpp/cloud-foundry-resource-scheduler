package de.grimmpp.AppManager.helper;

import de.grimmpp.AppManager.service.IServicePlan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServicePlanFinder {

    private static Map<String,IServicePlan> servicePlanMap = new HashMap<>();

    public static void registerServicePlan(String planId, IServicePlan service) {
        servicePlanMap.put(planId, service);
    }

    public static IServicePlan findServicePlan(String planId) {
        return servicePlanMap.get(planId);
    }

    public static List<IServicePlan> getServicePlans() {
        return servicePlanMap.values().stream().collect(Collectors.toList());
    }
}
