package de.grimmpp.AppManager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.helper.ObjectMapperFactory;
import de.grimmpp.AppManager.model.VcapApplication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class AppConfig {

    private ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Value("${logging.level.de.grimmpp.AppManager}")
    private String projectLogLevel;

    @Bean
    @Qualifier("ProjectLogLevel")
    public String getProjectLogLevel() {
        return projectLogLevel.toUpperCase();
    }

    @Value("${VCAP_APPLICATION}")
    private String vcapApp;

    @Bean
    public VcapApplication getVcapApplication() throws IOException {
        return objectMapper.readValue(vcapApp, VcapApplication.class);
    }
}
