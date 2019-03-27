package de.grimmpp.AppManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface IServicePlan {
    ObjectMapper getObjectMapper();
    String getServicePlanId();
    void run() throws IOException;
}
