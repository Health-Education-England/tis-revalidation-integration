/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.router.service;

import java.util.concurrent.ExecutorService;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.AggregationKey;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.EnrichedConnectionsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.JsonStringAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.processor.AttachNotesToConnectionProcessor;
import uk.nhs.hee.tis.revalidation.integration.router.processor.MergeEnrichedConnectionsIntoSummaryProcessor;

@Component
public class ConnectionServiceRouter extends RouteBuilder {

  private static final String API_CONNECTION_ADD = "/api/connections/add?bridgeEndpoint=true";
  private static final String API_CONNECTION_REMOVE = "/api/connections/remove?bridgeEndpoint=true";
  private static final String API_DISCREPANCY_HIDDEN =
      "/api/connections/discrepancies/hidden?bridgeEndpoint=true";
  private static final String API_DISCREPANCY_SHOW =
      "/api/connections/discrepancies/hidden/${header.discrepancyId}?bridgeEndpoint=true";
  private static final String API_DISCREPANCY_HIDDEN_DETAILS =
      "/api/connections/discrepancies/hidden/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_CONNECTION_EXCEPTION =
      "/api/connections/exception?bridgeEndpoint=true";
  private static final String API_CONNECTION_DISCREPANCIES =
      "/api/connections/discrepancies?bridgeEndpoint=true";
  private static final String API_CONNECTION_CONNECTED =
      "/api/connections/connected?bridgeEndpoint=true";
  private static final String API_DOCTORS_DESIGNATED_BODY_BY_GMC_ID =
      "/api/v1/doctors/designated-body/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_CONNECTION_HISTORY =
      "/api/connections/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_CONNECTION_EXCEPTIONLOG_TODAY =
      "/api/exceptionLog/today?bridgeEndpoint=true";
  private static final String ENRICH_CONNECTED_SUMMARY_WITH_NOTES =
      "direct:enrich-connected-summary-with-notes";
  private static final AggregationStrategy AGGREGATOR = new JsonStringAggregationStrategy();
  private final ExecutorService notesExecutor;
  private final EnrichedConnectionsAggregationStrategy enrichedConnectionsAggregationStrategy;
  private final AttachNotesToConnectionProcessor attachNotesToConnectionProcessor;
  private final MergeEnrichedConnectionsIntoSummaryProcessor
      mergeEnrichedConnectionsIntoSummaryProcessor;

  @Value("${service.tcs.url}")
  private String tcsServiceUrl;

  @Value("${service.recommendation.url}")
  private String recommendationServiceUrl;

  @Value("${service.reference.url}")
  private String serviceUrlReference;

  @Value("${service.connection.url}")
  private String serviceUrlConnection;

  /**
   * Constructor of ConnectionServiceRouter.
   */
  public ConnectionServiceRouter(@Qualifier("notesExecutor") ExecutorService notesExecutor,
      EnrichedConnectionsAggregationStrategy enrichedConnectionsAggregationStrategy,
      AttachNotesToConnectionProcessor attachNotesToConnectionProcessor,
      MergeEnrichedConnectionsIntoSummaryProcessor mergeEnrichedConnectionsIntoSummaryProcessor) {
    this.notesExecutor = notesExecutor;
    this.enrichedConnectionsAggregationStrategy = enrichedConnectionsAggregationStrategy;
    this.attachNotesToConnectionProcessor = attachNotesToConnectionProcessor;
    this.mergeEnrichedConnectionsIntoSummaryProcessor =
        mergeEnrichedConnectionsIntoSummaryProcessor;
  }

  @Override
  public void configure() {

    // Connection summary page - exception queue tab
    from("direct:connection-exception-summary")
        .to(serviceUrlConnection + API_CONNECTION_EXCEPTION)
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection summary page - Discrepancies queue tab
    from("direct:connection-discrepancies-summary")
        .to(serviceUrlConnection + API_CONNECTION_DISCREPANCIES)
        .unmarshal().json(JsonLibrary.Jackson, ConnectionSummaryDto.class)
        .to(ENRICH_CONNECTED_SUMMARY_WITH_NOTES);

    // Connection summary page - Connected queue tab
    from("direct:connection-connected-summary")
        .to(serviceUrlConnection + API_CONNECTION_CONNECTED)
        .unmarshal().json(JsonLibrary.Jackson, ConnectionSummaryDto.class)
        .to(ENRICH_CONNECTED_SUMMARY_WITH_NOTES);

    // Hidden Discrepancies page - Hidden Discrepancies tab
    from("direct:connection-hidden-discrepancies-summary")
        .to(serviceUrlConnection + API_DISCREPANCY_HIDDEN);

    // TODO: Change to use tis-revalidation-core when deployed.
    from("direct:v1-doctors")
        .to(recommendationServiceUrl + "/api/v1/doctors?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection Details page
    from("direct:connection-gmc-id-aggregation")
        .multicast(AGGREGATOR)
        .parallelProcessing()
        .to("direct:doctor-designated-body")
        .to("direct:connection-history")
        .to("direct:doctor-hidden-discrepancies");
    from("direct:doctor-designated-body")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.DESIGNATED_BODY_CODE)
        .toD(recommendationServiceUrl + API_DOCTORS_DESIGNATED_BODY_BY_GMC_ID);
    from("direct:connection-history")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.CONNECTION)
        .toD(serviceUrlConnection + API_CONNECTION_HISTORY);
    from("direct:doctor-hidden-discrepancies")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.HIDDEN_DISCREPANCIES)
        .toD(serviceUrlConnection + API_DISCREPANCY_HIDDEN_DETAILS);

    // Add connection
    from("direct:connection-add")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrlConnection + API_CONNECTION_ADD);

    // Remove connection
    from("direct:connection-remove")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrlConnection + API_CONNECTION_REMOVE);

    // Hide discrepancy
    from("direct:connection-discrepancies-hide")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrlConnection + API_DISCREPANCY_HIDDEN);

    // Show discrepancy
    from("direct:connection-discrepancies-show")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.DELETE))
        .toD(serviceUrlConnection + API_DISCREPANCY_SHOW);

    // Connection Exception Logs
    from("direct:connection-exception-log-today")
        .toD(serviceUrlConnection + API_CONNECTION_EXCEPTIONLOG_TODAY)
        .unmarshal().json(JsonLibrary.Jackson);

    // Enrich connected summary with notes
    from(ENRICH_CONNECTED_SUMMARY_WITH_NOTES)
        .setProperty("connected_summary", body())
        .split(simple("${exchangeProperty.connected_summary.connections}"))
        .executorService(notesExecutor)
        .setProperty("connection", body())
        .setHeader("gmcId", simple("${exchangeProperty.connection.gmcReferenceNumber}"))
        .to("direct:traineenotes-get")
        .choice()
        .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
        .unmarshal().json(JsonLibrary.Jackson, TraineeNotesDto.class)
        .endChoice()
        .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(404))
        .setBody(constant((Object) null))
        .otherwise()
        .log(
            "Unexpected notes response for gmcId=${header.gmcId}, "
                + "status=${header.CamelHttpResponseCode}")
        .setBody(constant((Object) null))
        .end()
        .process(attachNotesToConnectionProcessor)
        .end()
        .aggregate(constant(true), enrichedConnectionsAggregationStrategy)
        .completionPredicate(exchangeProperty(Exchange.SPLIT_COMPLETE).isEqualTo(true))
        .process(mergeEnrichedConnectionsIntoSummaryProcessor);
  }
}
