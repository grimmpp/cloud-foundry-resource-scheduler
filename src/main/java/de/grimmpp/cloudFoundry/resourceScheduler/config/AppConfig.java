package de.grimmpp.cloudFoundry.resourceScheduler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.model.VcapApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class AppConfig {

    public static final String HEADER_NAME_CF_SENDER_APP = "X-CF-SENDER-APP-INSTANCE";
    public String getSenderAppHttpHeaderValue() {
        return getVcapApplication().getApplication_id()+":"+getCfInstanceIndex();
    }

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Value("${logging.level.de.grimmpp.cloudFoundry.resourceScheduler}")
    private String projectLogLevel;

    @Bean
    @Qualifier("ProjectLogLevel")
    public String getProjectLogLevel() {
        return projectLogLevel.toUpperCase();
    }

    @Value("${VCAP_APPLICATION}")
    private String vcapAppStr;
    private VcapApplication vcapApp = null;
    @Bean
    public VcapApplication getVcapApplication() {
        if (vcapApp == null) {
            try {
                vcapApp = objectMapper.readValue(vcapAppStr, VcapApplication.class);
            } catch (IOException e) {
                log.error("Cannot convert VCAP_APPLICATION to object.", e);
            }
        }
        return vcapApp;
    }


    private Integer amountOfInstances = null;

    /**
     * This value must be set first by making a call to the CF API. That is done in the scheduler.
     * @return amount of configured instances of this app.
     */
    public Integer getAmountOfInstances() {
        return amountOfInstances;
    }

    public void updateAmountOfInstances(int instances) {
        amountOfInstances = instances;
    }

    @Value("${CF_INSTANCE_INDEX}")
    private Integer cfInstanceIndex;
    public Integer getCfInstanceIndex() {
        return cfInstanceIndex;
    }

    @Value("${scheduling-enabled}")
    private Boolean schedulingEnabled;
    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }
}
