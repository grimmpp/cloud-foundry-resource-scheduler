package de.grimmpp.AppManager.service;

import java.io.IOException;

public interface IServicePlan {
    String getServicePlanId();
    void run() throws IOException;
}
