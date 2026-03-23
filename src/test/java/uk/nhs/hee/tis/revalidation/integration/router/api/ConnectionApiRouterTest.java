/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.router.api;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.apache.camel.builder.Builder.constant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the Apache Camel routes defined in
 * {@link uk.nhs.hee.tis.revalidation.integration.router.service.ConnectionServiceRouter}.
 *
 */
@Testcontainers
@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConnectionApiRouterTest {

  private static final String MOCK_SERVICE = "mock:service";
  private static final String MOCK_RESULT = "mock:result";
  private static final String MOCK_RESPONSE_BODY = "{data=mocked}";

  @Container
  static ElasticsearchContainer elasticsearchTestContainer = new ElasticsearchContainer(
      "docker.elastic.co/elasticsearch/elasticsearch:7.4.2")
      .withExposedPorts(9200);

  @Container
  static RabbitMQContainer rabbitMQTestContainer = new RabbitMQContainer(
      "rabbitmq:3.8.9-management-alpine")
      .withExposedPorts(15672);

  @Autowired
  private CamelContext camelContext;

  @Autowired
  private ProducerTemplate producerTemplate;

  @EndpointInject(MOCK_SERVICE)
  private MockEndpoint mockService;

  @EndpointInject(MOCK_RESULT)
  private MockEndpoint mockResult;

  /**
   * Registers the dynamically-allocated container ports as Spring properties so that the
   * application context binds to the Testcontainers instances instead of real services.
   *
   * @param registry the dynamic property registry provided by the Spring test framework
   */
  @DynamicPropertySource
  static void dataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbit.port", rabbitMQTestContainer::getAmqpPort);
    registry.add("spring.elasticsearch.rest.uris",
        elasticsearchTestContainer::getHttpHostAddress);
  }

  @BeforeAll
  static void setUpContainers() {

    elasticsearchTestContainer.start();
    rabbitMQTestContainer.start();
  }

  @BeforeEach
  void setUp() {
    mockService.reset();
    mockResult.reset();
  }

  @AfterAll
  static void tearDownContainers() {
    elasticsearchTestContainer.stop();
    rabbitMQTestContainer.stop();
  }

  /**
   * Verifies that a message sent to each route is correctly routed to the downstream HTTP service.
   *
   * @throws Exception if the Camel context fails to start or an assertion is violated
   */
  @ParameterizedTest
  @ValueSource(strings = {
      "connection-exception-summary",
      "connection-discrepancies-summary",
      "connection-connected-summary",
      "connection-disconnected-summary",
      "connection-exception-log-today",
      "connection-add",
      "connection-remove",
      "connection-discrepancies-hide"
  })
  void shouldRouteConnectionApiCalls(String route) throws Exception {
    // Replace the downstream HTTP call with a mock that returns a JSON response
    adviceWith(camelContext, route, a -> {
      a.weaveByToUri("http*").replace().to(MOCK_SERVICE);
    });
    camelContext.start();

    mockService.expectedMessageCount(1);
    mockService.returnReplyBody(constant(MOCK_RESPONSE_BODY));

    Exchange result = producerTemplate.request(String.format("direct:%s", route),
        e -> e.getMessage().setBody(null));

    mockService.assertIsSatisfied();
    assertThat(result.getMessage().getBody(String.class), is(MOCK_RESPONSE_BODY));
  }

  /**
   * Verifies that {@code direct:connection-summary} correctly delegates to its sub-routes.
   *
   * @throws Exception if the Camel context fails to start or an assertion is violated
   */
  @Test
  void shouldRouteToConnectionSummary() throws Exception {
    adviceWith(camelContext, "connection-hidden-manually", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    adviceWith(camelContext, "v1-doctors-all-unhidden", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    adviceWith(camelContext, "tcs-connection", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    camelContext.start();

    mockService.expectedMessageCount(1);
    mockService.returnReplyBody(constant(MOCK_RESPONSE_BODY));

    producerTemplate.request("direct:connection-summary",
        e -> e.getMessage().setBody(null));

    mockService.assertIsSatisfied();
  }

  /**
   * Verifies that {@code direct:connection-gmc-id-aggregation} correctly routes to its sub-routes.
   *
   * @throws Exception if the Camel context fails to start or an assertion is violated
   */
  @Test
  void shouldRouteGmcIdAggregation() throws Exception {
    adviceWith(camelContext, "doctor-designated-body", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    adviceWith(camelContext, "connection-history", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    camelContext.start();

    mockService.expectedMessageCount(2);
    mockService.returnReplyBody(constant(MOCK_RESPONSE_BODY));

    producerTemplate.request("direct:connection-gmc-id-aggregation",
        e -> e.getMessage().setBody(null));

    mockService.assertIsSatisfied();
  }

  /**
   * Verifies that {@code direct:connection-hidden} correctly delegates to its sub-routes.
   *
   * @throws Exception if the Camel context fails to start or an assertion is violated
   */
  @Test
  void shouldRouteConnectionHidden() throws Exception {
    adviceWith(camelContext, "connection-hidden-gmcIds", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    adviceWith(camelContext, "v1-doctors-by-ids", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    adviceWith(camelContext, "connection-tcs-hidden", a ->
        a.weaveByToUri("http*").replace().to(MOCK_SERVICE));
    camelContext.start();

    mockService.expectedMessageCount(1);
    mockService.returnReplyBody(constant(MOCK_RESPONSE_BODY));

    producerTemplate.request("direct:connection-hidden",
        e -> e.getMessage().setBody(null));

    mockService.assertIsSatisfied();
  }
}
