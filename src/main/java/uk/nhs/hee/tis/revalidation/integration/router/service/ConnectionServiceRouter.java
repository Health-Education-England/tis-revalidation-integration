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

import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.GET_TOKEN_METHOD;
import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.OIDC_ACCESS_TOKEN_HEADER;

import java.util.Map;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.AggregationKey;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.ConnectionExceptionAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.ConnectionHiddenAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorConnectionAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.JsonStringAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class ConnectionServiceRouter extends RouteBuilder {

  private static final String API_CONNECTION =
      "/api/revalidation/connection/${header.gmcIds}?bridgeEndpoint=true";
  private static final String API_CONNECTION_ADD = "/api/connections/add?bridgeEndpoint=true";
  private static final String API_CONNECTION_REMOVE = "/api/connections/remove?bridgeEndpoint=true";
  private static final String API_CONNECTION_HIDE = "/api/connections/hide?bridgeEndpoint=true";
  private static final String API_CONNECTION_UNHIDE = "/api/connections/unhide?bridgeEndpoint=true";
  private static final String API_CONNECTION_HIDDEN = "/api/connections/hidden?bridgeEndpoint=true";
  private static final String API_CONNECTION_EXCEPTION =
      "/api/connections/exception?bridgeEndpoint=true";
  private static final String API_CONNECTION_DISCREPANCIES =
      "/api/connections/discrepancies?bridgeEndpoint=true";
  private static final String API_CONNECTION_CONNECTED =
      "/api/connections/connected?bridgeEndpoint=true";
  private static final String API_CONNECTION_DISCONNECTED =
      "/api/connections/disconnected?bridgeEndpoint=true";
  private static final String API_CONNECTION_TCS_HIDDEN =
      "/api/revalidation/connection/hidden/${header.gmcIds}?searchQuery=${header.searchQuery}"
          + "&pageNumber=${header.pageNumber}&bridgeEndpoint=true";
  private static final String API_CONNECTION_DOCTOR_UNHIDDEN =
      "/api/v1/doctors/unhidden/${header.gmcIds}?bridgeEndpoint=true";
  private static final String API_DOCTORS_DESIGNATED_BODY_BY_GMC_ID =
      "/api/v1/doctors/designated-body/${header.gmcId}?bridgeEndpoint=true";
  private static final String GET_DOCTORS_BY_GMC_IDS =
      "/api/v1/doctors/gmcIds/${header.gmcIds}?bridgeEndpoint=true";
  private static final String API_CONNECTION_HISTORY =
      "/api/connections/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_CONNECTION_EXCEPTIONLOG_TODAY =
      "/api/exceptionLog/today?bridgeEndpoint=true";

  private static final AggregationStrategy AGGREGATOR = new JsonStringAggregationStrategy();
  public static final String GMC_IDS_HEADER = "gmcIds";

  @Autowired
  private KeycloakBean keycloakBean;

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private DoctorConnectionAggregationStrategy doctorConnectionAggregationStrategy;

  @Autowired
  private ConnectionHiddenAggregationStrategy connectionHiddenAggregationStrategy;

  @Autowired
  private ConnectionExceptionAggregationStrategy connectionExceptionAggregationStrategy;

  @Value("${service.tcs.url}")
  private String tcsServiceUrl;

  @Value("${service.recommendation.url}")
  private String recommendationServiceUrl;

  @Value("${service.reference.url}")
  private String serviceUrlReference;

  @Value("${service.connection.url}")
  private String serviceUrlConnection;

  @Override
  public void configure() {

    // Connection summary page - All, Connected, Disconnected tab
    from("direct:connection-summary")
        .to("direct:connection-hidden-manually")
        .setHeader(GMC_IDS_HEADER).method(gmcIdProcessorBean, "getHiddenGmcIds")
        .to("direct:v1-doctors-all-unhidden")
        .setHeader(GMC_IDS_HEADER).method(gmcIdProcessorBean, "process")
        .enrich("direct:tcs-connection", doctorConnectionAggregationStrategy);
    from("direct:connection-hidden-manually")
        .to(serviceUrlConnection + API_CONNECTION_HIDDEN);
    from("direct:v1-doctors-all-unhidden")
        .toD(recommendationServiceUrl + API_CONNECTION_DOCTOR_UNHIDDEN)
        .streamCaching()
        .unmarshal().json(JsonLibrary.Jackson, Map.class);
    from("direct:tcs-connection")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(tcsServiceUrl + API_CONNECTION)
        .unmarshal().json(JsonLibrary.Jackson, Map.class);

    // Connection summary page - exception queue tab
    from("direct:connection-exception-summary")
        .to(serviceUrlConnection + API_CONNECTION_EXCEPTION)
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection summary page - Discrepancies queue tab
    from("direct:connection-discrepancies-summary")
        .to(serviceUrlConnection + API_CONNECTION_DISCREPANCIES)
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection summary page - Connected queue tab
    from("direct:connection-connected-summary")
        .to(serviceUrlConnection + API_CONNECTION_CONNECTED)
        .unmarshal().json(JsonLibrary.Jackson);

    // Disconnection summary page - Disconnected queue tab
    from("direct:connection-disconnected-summary")
        .to(serviceUrlConnection + API_CONNECTION_DISCONNECTED)
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection summary page - Hidden tab
    from("direct:connection-hidden")
        .to("direct:connection-hidden-gmcIds")
        .setHeader(GMC_IDS_HEADER).method(gmcIdProcessorBean, "getHiddenGmcIds")
        .to("direct:v1-doctors-by-ids")
        .enrich("direct:connection-tcs-hidden", connectionHiddenAggregationStrategy);
    from("direct:connection-hidden-gmcIds")
        .to(serviceUrlConnection + API_CONNECTION_HIDDEN);
    from("direct:connection-tcs-hidden")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(tcsServiceUrl + API_CONNECTION_TCS_HIDDEN)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:v1-doctors-by-ids")
        .toD(recommendationServiceUrl + GET_DOCTORS_BY_GMC_IDS)
        .unmarshal().json(JsonLibrary.Jackson);

    // TODO: Change to use tis-revalidation-core when deployed.
    from("direct:v1-doctors")
        .to(recommendationServiceUrl + "/api/v1/doctors?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);

    // Connection Details page
    from("direct:connection-gmc-id-aggregation")
        .multicast(AGGREGATOR)
        .parallelProcessing()
        .to("direct:doctor-designated-body")
        .to("direct:connection-history");
    from("direct:doctor-designated-body")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.DESIGNATED_BODY_CODE)
        .toD(recommendationServiceUrl + API_DOCTORS_DESIGNATED_BODY_BY_GMC_ID);
    from("direct:connection-history")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.CONNECTION)
        .toD(serviceUrlConnection + API_CONNECTION_HISTORY);

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

    // Hide connection
    from("direct:connection-hide")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrlConnection + API_CONNECTION_HIDE);

    // Unhide connection
    from("direct:connection-unhide")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrlConnection + API_CONNECTION_UNHIDE);

    // Connection Exception Logs
    from("direct:connection-exception-log-today")
        .toD(serviceUrlConnection + API_CONNECTION_EXCEPTIONLOG_TODAY)
        .unmarshal().json(JsonLibrary.Jackson);
  }
}
