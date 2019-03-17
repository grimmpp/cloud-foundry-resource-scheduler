package grimmpp.AppManager.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @LocalServerPort
    int randomServerPort;

    @Bean
    @Qualifier("baseUrl")
    public String getBaseUrl() {
        return "http://localhost:"+randomServerPort;
    }
}
