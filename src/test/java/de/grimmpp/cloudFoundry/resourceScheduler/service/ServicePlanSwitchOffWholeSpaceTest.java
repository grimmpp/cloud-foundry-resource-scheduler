package de.grimmpp.cloudFoundry.resourceScheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.grimmpp.cloudFoundry.resourceScheduler.AppManagerApplication;
import de.grimmpp.cloudFoundry.resourceScheduler.helper.ObjectMapperFactory;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.AbstractMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.mocks.CfApiMockController;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.Parameter;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ParameterRepository;
import de.grimmpp.cloudFoundry.resourceScheduler.model.database.ServiceInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ServicePlanSwitchOffWholeSpaceTest {

    private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    @Autowired
    private ParameterRepository pRepo;

    @Autowired
    private ServicePlanSwitchOffWholeSpace servicePlan;

    @Autowired
    private CfApiMockController cfApiMockController;

    private String siid = UUID.randomUUID().toString();
    private String spaceId = "162dc5a3-ddb9-41bc-9fb0-38cc7aec73f9";
    private String productionSpaceId = "bc8d3381-390d-4bd7-8c71-25309900a2e3";

    @Test
    public void productionSpaceNameTest() throws IOException, ParseException {
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(productionSpaceId)
                .build();

        long hours = TimeParameterValidator.getHours(System.currentTimeMillis());
        long minutes = TimeParameterValidator.getMinutes(System.currentTimeMillis()-60*1000); //-1min
        String[] timesStr = new String[]{ hours+":"+minutes };

        pRepo.save(new Parameter(siid, Parameter.KEY_TIMES, objectMapper.writeValueAsString(timesStr)));
        pRepo.save(new Parameter(siid, Parameter.KEY_LAST_CALL, "0"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.GET.toString(), httpMethod);

        String url = AbstractMockController.BASE_URL + "/v2/spaces/bc8d3381-390d-4bd7-8c71-25309900a2e3?order-direction=asc&results-per-page=100&page=1";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "";
        Assert.assertEquals(requestBody, cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }

    @Test
    public void stopAllAppsAtASpecificTimeTest() throws IOException {
        ServiceInstance si = ServiceInstance.builder()
                .serviceInstanceId(siid)
                .spaceId(spaceId)
                .build();

        long hours = TimeParameterValidator.getHours(System.currentTimeMillis());
        long minutes = TimeParameterValidator.getMinutes(System.currentTimeMillis()-60*1000); //-1min
        String[] timesStr = new String[]{ hours+":"+minutes };

        pRepo.save(new Parameter(siid, Parameter.KEY_TIMES, objectMapper.writeValueAsString(timesStr)));
        pRepo.save(new Parameter(siid, Parameter.KEY_LAST_CALL, "0"));

        // execute logic to test
        servicePlan.performActionForServiceInstance(si);

        // Check last call which stopped the app
        String httpMethod = cfApiMockController.getLastOperation(CfApiMockController.KEY_HTTP_METHOD);
        Assert.assertEquals(RequestMethod.PUT.toString(), httpMethod);

        String url = AbstractMockController.BASE_URL + "/v2/apps/15b3885d-0351-4b9b-8697-86641668c123?order-direction=asc&results-per-page=100&page=1";
        Assert.assertEquals(url, cfApiMockController.getLastOperation(CfApiMockController.KEY_URL));

        String requestBody = "{\"state\": \"STOPPED\"}";
        Assert.assertEquals(requestBody, cfApiMockController.getLastOperation(CfApiMockController.KEY_REQUEST_BODY));
    }

}
