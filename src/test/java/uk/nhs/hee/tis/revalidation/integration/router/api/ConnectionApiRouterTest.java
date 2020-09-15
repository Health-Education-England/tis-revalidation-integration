package uk.nhs.hee.tis.revalidation.integration.router.api;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionSummaryDto;

@SpringBootTest
@RunWith(CamelSpringBootRunner.class)
@MockEndpoints("direct:connection-summary")
public class ConnectionApiRouterTest {

  @EndpointInject(uri = "mock:direct:connection-summary")
  MockEndpoint mockEndpoint;

  @Autowired
  ProducerTemplate producerTemplate;

  @Test
  public void connectionApiTest() throws InterruptedException {

    List< ConnectionSummaryDto> connectionSummaryDtoList = new ArrayList<>();
    mockEndpoint.expectedBodiesReceived(connectionSummaryDtoList);
    producerTemplate.sendBody("/connection", null);
    mockEndpoint.assertIsSatisfied();
  }

}
