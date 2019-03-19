package grimmpp.AppManager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import grimmpp.AppManager.model.VcapApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class AppConfig {

    @Value("${VCAP_APPLICATION}")
    private String vcapApp;

    @Bean
    public VcapApplication getVcapApplication() throws IOException {
        return new ObjectMapper().readValue(vcapApp, VcapApplication.class);
    }
}
