package grimmpp.AppManager;

import com.zaxxer.hikari.HikariDataSource;
import grimmpp.AppManager.model.database.ServiceInstance;
import grimmpp.AppManager.model.database.ServiceInstanceRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DatabaseTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Value("${spring.jpa.database-platform}")
    private String dbPlatform;

    @Autowired
    DataSource dataSource;

    @Test
    public void datasourceTest() {
        Assert.assertEquals("org.hibernate.dialect.H2Dialect", dbPlatform);

        Assert.assertNotNull(dataSource);

        Assert.assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hds = (HikariDataSource)dataSource;

        Assert.assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1",hds.getJdbcUrl());
        Assert.assertEquals("org.h2.Driver",hds.getDriverClassName());
        Assert.assertEquals("HikariPool-1",hds.getPoolName());
        Assert.assertEquals("sa",hds.getUsername());
        Assert.assertEquals("sa",hds.getPassword());
    }

    @Test
    public void readWriteDBTest() {
        String id = UUID.randomUUID().toString();
        String planId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        ServiceInstance si = new ServiceInstance();
        si.setServiceInstanceId(id);
        si.setServicePlanId(planId);
        si.setPropertyTime(time);
        si.setSpaceId(spaceId);
        si.setOrgId(orgId);
        serviceInstanceRepository.save(si);

        si = serviceInstanceRepository.findById(id).get();
        Assert.assertNotNull(si);
        Assert.assertEquals(planId, si.getServicePlanId());
        Assert.assertEquals(spaceId, si.getSpaceId());
        Assert.assertEquals(orgId, si.getOrgId());
        Assert.assertEquals(time, si.getPropertyTime());

        si = serviceInstanceRepository.findByServiceInstanceId(id);
        Assert.assertNotNull(si);
    }
}
