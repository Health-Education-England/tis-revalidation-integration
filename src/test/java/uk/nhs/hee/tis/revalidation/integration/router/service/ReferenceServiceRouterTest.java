package uk.nhs.hee.tis.revalidation.integration.router.service;

import static org.apache.camel.builder.Builder.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.EnableRouteCoverage;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.integration.RevalidationIntegrationApplication;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ReferenceServiceRouterTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CamelContext camelContext;

  @EndpointInject("mock:direct:reference-dbcs")
  protected MockEndpoint resultEndpoint;

//  @Produce("direct:reference")
//  protected ProducerTemplate template;

  @BeforeEach
  public void setUp() throws Exception {
    AdviceWith.adviceWith(camelContext, "getReferenceDbcs", a -> {
      a.mockEndpointsAndSkip("*/api/dbcs?bridgeEndpoint=true/*");
    });

    resultEndpoint.returnReplyBody(constant("User"));
  }


  @Test
  public void shouldGetReferenceData() throws InterruptedException {
    resultEndpoint.expectedBodiesReceived("David");
   // final var template = new TestRestTemplate();
    final var entity = restTemplate.getForEntity("/api/reference/dbcs", String.class);
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
//    resultEndpoint.expectedBodiesReceived("David");
//    template.sendBody("David");
//    resultEndpoint.assertIsSatisfied();
  }

}
