/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.AggregationKey;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.JsonStringAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.TraineeNotesAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.exception.ExceptionHandlerProcessor;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.TraineeDetailProcessor;

@Component
public class TraineeServiceRouter extends RouteBuilder {

  private static final String API_TRAINEE =
      "/api/revalidation/trainee/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_TRAINEES =
      "/api/revalidation/trainees/${header.gmcIds}?bridgeEndpoint=true";
  private static final String API_TRAINEENOTES =
      "/api/trainee/${header.gmcId}/notes?bridgeEndpoint=true";
  private static final String API_TRAINEEENOTES_ADD =
      "/api/trainee/notes/add?bridgeEndpoint=true";
  private static final String API_TRAINEEENOTES_EDIT =
      "/api/trainee/notes/edit?bridgeEndpoint=true";
  private static final String GET_DOCTORS_BY_GMC_IDS =
      "/api/v1/doctors/gmcIds/${header.gmcIds}?bridgeEndpoint=true";
  private static final AggregationStrategy AGGREGATOR = new JsonStringAggregationStrategy();

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

  @Value("${service.recommendation.url}")
  private String recommendationServiceUrl;

  @Autowired
  private TraineeDetailProcessor traineeProcessor;

  @Autowired
  private ExceptionHandlerProcessor exceptionHandlerProcessor;

  @Override
  public void configure() {

    from("direct:trainee")
        .multicast(AGGREGATOR)
        .parallelProcessing()
        .to("direct:trainee-details")
        .to("direct:traineenotes-get")
        .to("direct:gmc-doctors-by-ids")
        .end()
        .process(traineeProcessor);

    from("direct:trainee-details")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant("programme")
        .doTry()
        .toD(serviceUrl + API_TRAINEE)
        .doCatch(HttpOperationFailedException.class)
        .process(exchange -> {
          var e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
              HttpOperationFailedException.class);
          int statusCode = e.getHttpResponseCode();
          if (HttpStatus.NOT_FOUND.value() == statusCode) {
            exchange.getIn().setBody("{}");
          } else {
            throw e;
          }
        });

    from("direct:traineenotes-get")
        .setHeader(AggregationKey.HEADER).constant("notes")
        .toD(coreServiceUrl + API_TRAINEENOTES);

    from("direct:gmc-doctors-by-ids")
        .setHeader(AggregationKey.HEADER).constant("doctor")
        .setHeader("gmcIds").simple("${header.gmcId}")
        .toD(recommendationServiceUrl + GET_DOCTORS_BY_GMC_IDS);

    from("direct:traineenotes-add")
        .to(coreServiceUrl + API_TRAINEEENOTES_ADD);
    from("direct:traineenotes-edit")
        .to(coreServiceUrl + API_TRAINEEENOTES_EDIT);
    from("direct:trainees")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_TRAINEES);
  }
}
