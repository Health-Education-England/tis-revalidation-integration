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

import java.util.Map;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorConnectionAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class ConnectionServiceRouter extends RouteBuilder {

  private static final String API_CONNECTION = "/api/revalidation/connection/${header.gmcIds}?bridgeEndpoint=true";
  private static final String OIDC_ACCESS_TOKEN_HEADER = "OIDC_access_token";
  private static final String GET_TOKEN_METHOD = "getAuthToken";

  @Autowired
  private KeycloakBean keycloakBean;

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private DoctorConnectionAggregationStrategy doctorConnectionAggregationStrategy;

  @Value("${service.tcs.url}")
  private String tcsServiceUrl;

  @Value("${service.recommendation.url}")
  private String recommendationServiceUrl;

  @Override
  public void configure() {

    from("direct:connection-summary")
        .to("direct:v1-doctors")
        .setHeader("gmcIds").method(gmcIdProcessorBean, "process")
        .enrich("direct:tcs-connection", doctorConnectionAggregationStrategy);

    // TODO: Change to use tis-revalidation-core when deployed.
    from("direct:v1-doctors")
        .to(recommendationServiceUrl + "/api/v1/doctors?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:tcs-connection")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(tcsServiceUrl + API_CONNECTION)
        .unmarshal().json(JsonLibrary.Jackson, Map.class);
  }
}
