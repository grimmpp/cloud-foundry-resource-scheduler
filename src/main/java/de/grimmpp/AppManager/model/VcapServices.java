package de.grimmpp.AppManager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcapServices extends HashMap<String, List<ServiceInfo>> {

    public ServiceInfo getServiceInfo(String serviceInstanceName) {
        List<ServiceInfo> serviceInfos = values().stream().flatMap(v -> v.stream()).collect(Collectors.toList());
        for(ServiceInfo si: serviceInfos) {
            if (si.getInstance_name().toLowerCase().equals(serviceInstanceName.toLowerCase())) {
                return si;
            }
        }
        return null;
    }
}
