package de.grimmpp.cloudFoundry.resourceScheduler;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CatalogTest {

    @Autowired
    private Catalog catalog;

    @Value("${trim-catalog-descriptions:false}")
    private Boolean trimCatalogDescriptions;

    @Test
    public void planTest() {
        Assert.assertEquals(1, catalog.getServiceDefinitions().size());

        ServiceDefinition sd = catalog.getServiceDefinitions().get(0);
        Assert.assertNotNull(sd);

        Assert.assertEquals(36, UUID.fromString(sd.getId()).toString().length());
        Assert.assertTrue(sd.getName().length() > 0);
        Assert.assertTrue(sd.getDescription().length() > 0);
        Assert.assertTrue(sd.getPlans().size() > 0);

        for (Plan p: sd.getPlans()) {
            Assert.assertEquals(36, UUID.fromString(p.getId()).toString().length());
            Assert.assertNotNull(p.getName());
            Assert.assertTrue(p.getName().length() > 0);
            Assert.assertTrue(p.getDescription().length() > 0);
            Assert.assertTrue(p.getMetadata().keySet().size() > 0);
        }
    }

    @Test
    public void testCatalogDescriptions() {
        Assert.assertTrue(trimCatalogDescriptions);

        ServiceDefinition sd = catalog.getServiceDefinitions().get(0);
        Assert.assertTrue(sd.getDescription().length() < 255);

        for(String k: sd.getMetadata().keySet()) {
            Assert.assertTrue(sd.getMetadata().get(k).toString().length() < 255);
        }

        for (Plan p: sd.getPlans()) {
            Assert.assertTrue(p.getDescription().length() < 255);
            for(String k: p.getMetadata().keySet()) {
                Assert.assertTrue(p.getMetadata().get(k).toString().length() < 255);
            }
        }
    }
}
