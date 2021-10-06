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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.TraineeNotesAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class TraineeServiceRouter extends RouteBuilder {

  private static final String API_TRAINEE =
      "/api/revalidation/trainee/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_TRAINEES =
      "/api/revalidation/trainees/${header.gmcIds}?bridgeEndpoint=true";
  private static final String API_TRAINEENOTES =
      "/api/trainee/${header.gmcId}/notes?bridgeEndpoint=true";
  private static final String API_TRAINEEENOTES_ADD =
      "/api/trainee/${header.gmcId}/notes/add?bridgeEndpoint=true";

  @Autowired
  private KeycloakBean keycloakBean;

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private TraineeNotesAggregationStrategy traineeNotesAggregationStrategy;

  @Value("${service.tcs.url}")
  private String serviceUrl;

  @Value("${service.core.url}")
  private String coreServiceUrl;

  @Override
  public void configure() {

    from("direct:trainee")
        .to("direct:trainee-details")
        .setHeader("gmcId").method(gmcIdProcessorBean, "getGmcIdOfRecommendationTrainee")
        .enrich("direct:traineenotes-get", traineeNotesAggregationStrategy);
    from("direct:trainee-details")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_TRAINEE)
        .unmarshal().json(JsonLibrary.Jackson);
    from("direct:traineenotes-get")
        .toD(coreServiceUrl + API_TRAINEENOTES)
        .unmarshal().json(JsonLibrary.Jackson);
    from("direct:traineenotes-add")
        .to(coreServiceUrl + API_TRAINEEENOTES_ADD);
    from("direct:trainees")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_TRAINEES);
  }
}
