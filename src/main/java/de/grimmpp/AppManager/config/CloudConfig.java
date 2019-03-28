package de.grimmpp.AppManager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.helper.ObjectMapperFactory;
import de.grimmpp.AppManager.model.ServiceInfo;
import de.grimmpp.AppManager.model.VcapServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;

@Configuration
@Slf4j
public class CloudConfig {

    private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Value("${VCAP_SERVICES}")
    private String vcapServices;

    @Bean
    public VcapServices getServiceInfos() throws IOException {
        return objectMapper.readValue(vcapServices, VcapServices.class);
    }

    @Bean
    public DataSource inventoryDataSource(VcapServices services) {
        ServiceInfo si = services.getServiceInfo("scheduler-db");

        String driverClassName = "com.mysql.jdbc.Driver";
        if (Arrays.asList(si.getTags()).stream().anyMatch(t -> t.toLowerCase().equals("h2"))) {
            driverClassName = "org.h2.Driver";
        }

        DataSource ds = DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .username(si.getCredentials().get("username"))
                .password(si.getCredentials().get("password"))
                .url(si.getCredentials().get("uri"))
                .build();

        log.debug("created datasource: driverClass: {}");

        return ds;
    }
}
