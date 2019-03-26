package de.grimmpp.AppManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.AppManager.model.ServiceInfo;
import de.grimmpp.AppManager.model.VcapServices;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CloudConfigTest {

    @Value("${VCAP_SERVICES}")
    private String vcapServicesStr;

    private String vcapServicesStrMysql = "{\"p-mysql\":[{\"binding_name\":null,\"credentials\":{\"hostname\":\"10.144.0.17\",\"jdbcUrl\":\"jdbc:mysql://10.144.0.17:3306/cf_c9179b31_9203_4d59_b0f9_f0d9ba504b41?user=zUAiVYAhc34DHoB7\\u0026password=1Og8kySo6bePPBxg\",\"name\":\"cf_c9179b31_9203_4d59_b0f9_f0d9ba504b41\",\"password\":\"1Og8kySo6bePPBxg\",\"port\":3306,\"uri\":\"mysql://zUAiVYAhc34DHoB7:1Og8kySo6bePPBxg@10.144.0.17:3306/cf_c9179b31_9203_4d59_b0f9_f0d9ba504b41?reconnect=true\",\"username\":\"zUAiVYAhc34DHoB7\"},\"instance_name\":\"scheduler-db\",\"label\":\"p-mysql\",\"name\":\"mysql\",\"plan\":\"10mb\",\"provider\":null,\"syslog_drain_url\":null,\"tags\":[\"mysql\"],\"volume_mounts\":[]}]}";

    @Test
    public void vcapServiceMySqlTest() throws IOException {
        VcapServices vcapServices = new ObjectMapper().readValue(vcapServicesStrMysql, VcapServices.class);
        Assert.assertEquals(1, vcapServices.keySet().size());

        List<ServiceInfo> lSi = vcapServices.get("p-mysql");
        Assert.assertEquals(1, lSi.size());

        ServiceInfo si = vcapServices.getServiceInfo("scheduler-db");
        Assert.assertNotNull(si);
        Assert.assertEquals("mysql", si.getName());
        Assert.assertEquals("scheduler-db", si.getInstance_name());
        Assert.assertEquals("p-mysql", si.getLabel());
        Assert.assertEquals("10mb", si.getPlan());
        Assert.assertEquals("mysql", si.getTags()[0]);
        Assert.assertEquals("cf_c9179b31_9203_4d59_b0f9_f0d9ba504b41", si.getCredentials().get("name"));
        Assert.assertTrue(si.getCredentials().get("jdbcUrl").length() > 20);
        Assert.assertTrue(si.getCredentials().get("uri").length() > 20);
    }

    @Test
    public void vcapServiceH2Test() throws IOException {
        VcapServices vcapServices = new ObjectMapper().readValue(vcapServicesStr, VcapServices.class);
        Assert.assertEquals(1, vcapServices.keySet().size());

        List<ServiceInfo> lSi = vcapServices.get("h2");
        Assert.assertEquals(1, lSi.size());

        ServiceInfo si = vcapServices.getServiceInfo("scheduler-db");
        Assert.assertNotNull(si);
        Assert.assertEquals("h2", si.getName());
        Assert.assertEquals("scheduler-db", si.getInstance_name());
        Assert.assertEquals("h2", si.getLabel());
        Assert.assertEquals("10mb", si.getPlan());
        Assert.assertEquals("h2", si.getTags()[0]);
        Assert.assertEquals("cf_c9179b31_9203_4d59_b0f9_f0d9ba504b41", si.getCredentials().get("name"));
        Assert.assertTrue(si.getCredentials().get("uri").length() > 20);
    }
}
