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

package uk.nhs.hee.tis.revalidation.integration.router.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.EnrichedConnectionsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionSummaryDto;

class MergeEnrichedConnectionsIntoSummaryProcessorTest {

  private final MergeEnrichedConnectionsIntoSummaryProcessor testObj =
      new MergeEnrichedConnectionsIntoSummaryProcessor();

  @Test
  void shouldMergeEnrichedConnectionsIntoConnectionSummary() {
    CamelContext context = new DefaultCamelContext();
    Exchange exchange = new DefaultExchange(context);

    ConnectionSummaryDto summary = new ConnectionSummaryDto();
    summary.setCountTotal(2);
    summary.setCountUnderNotice(1);
    summary.setTotalPages(1);
    summary.setTotalResults(2);

    ConnectionInfoDto firstConnection = new ConnectionInfoDto();
    firstConnection.setGmcReferenceNumber("12345");
    firstConnection.setDoctorFirstName("John");
    firstConnection.setDoctorLastName("Smith");
    firstConnection.setNotes(true);

    ConnectionInfoDto secondConnection = new ConnectionInfoDto();
    secondConnection.setGmcReferenceNumber("67890");
    secondConnection.setDoctorFirstName("Jane");
    secondConnection.setDoctorLastName("Doe");
    secondConnection.setNotes(false);

    exchange.setProperty(
        MergeEnrichedConnectionsIntoSummaryProcessor.SUMMARY, summary);
    exchange.setProperty(
        EnrichedConnectionsAggregationStrategy.ENRICHED_CONNECTIONS,
        List.of(firstConnection, secondConnection));

    testObj.process(exchange);

    ConnectionSummaryDto result =
        exchange.getMessage().getBody(ConnectionSummaryDto.class);

    assertNotNull(result);
    assertEquals(2, result.getCountTotal());
    assertEquals(1, result.getCountUnderNotice());
    assertEquals(1, result.getTotalPages());
    assertEquals(2, result.getTotalResults());

    assertNotNull(result.getConnections());
    assertEquals(2, result.getConnections().size());

    assertEquals("12345", result.getConnections().get(0).getGmcReferenceNumber());
    assertEquals("John", result.getConnections().get(0).getDoctorFirstName());
    assertEquals("Smith", result.getConnections().get(0).getDoctorLastName());
    assertEquals(true, result.getConnections().get(0).getNotes());

    assertEquals("67890", result.getConnections().get(1).getGmcReferenceNumber());
    assertEquals("Jane", result.getConnections().get(1).getDoctorFirstName());
    assertEquals("Doe", result.getConnections().get(1).getDoctorLastName());
    assertEquals(false, result.getConnections().get(1).getNotes());
  }
}
