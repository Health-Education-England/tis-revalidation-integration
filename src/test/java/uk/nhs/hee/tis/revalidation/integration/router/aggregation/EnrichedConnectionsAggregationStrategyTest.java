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

package uk.nhs.hee.tis.revalidation.integration.router.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;

class EnrichedConnectionsAggregationStrategyTest {

  EnrichedConnectionsAggregationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new EnrichedConnectionsAggregationStrategy();
  }

  @Test
  void shouldAggregateConnectionToExistingList() {
    CamelContext context = new DefaultCamelContext();

    Exchange oldExchange = new DefaultExchange(context);
    ConnectionInfoDto firstConnection = new ConnectionInfoDto();
    firstConnection.setGmcReferenceNumber("12345");
    firstConnection.setDoctorFirstName("John");
    firstConnection.setDoctorLastName("Smith");

    oldExchange.setProperty(
        EnrichedConnectionsAggregationStrategy.ENRICHED_CONNECTIONS,
        new java.util.ArrayList<>(List.of(firstConnection)));

    Exchange newExchange = new DefaultExchange(context);
    ConnectionInfoDto secondConnection = new ConnectionInfoDto();
    secondConnection.setGmcReferenceNumber("12346");
    secondConnection.setDoctorFirstName("Jane");
    secondConnection.setDoctorLastName("Doe");

    newExchange.getMessage().setBody(secondConnection);

    Exchange result = strategy.aggregate(oldExchange, newExchange);

    assertNotNull(result);
    @SuppressWarnings("unchecked")
    List<ConnectionInfoDto> enrichedConnections =
        (List<ConnectionInfoDto>) result.getProperty(
            EnrichedConnectionsAggregationStrategy.ENRICHED_CONNECTIONS);

    assertNotNull(enrichedConnections);
    assertEquals(2, enrichedConnections.size());
    assertEquals("12345", enrichedConnections.get(0).getGmcReferenceNumber());
    assertEquals("John", enrichedConnections.get(0).getDoctorFirstName());
    assertEquals("Smith", enrichedConnections.get(0).getDoctorLastName());
    assertEquals("12346", enrichedConnections.get(1).getGmcReferenceNumber());
    assertEquals("Jane", enrichedConnections.get(1).getDoctorFirstName());
    assertEquals("Doe", enrichedConnections.get(1).getDoctorLastName());
  }
}
