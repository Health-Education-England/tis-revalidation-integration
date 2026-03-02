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
import java.util.concurrent.ExecutorService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorRecommendationAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.EnrichedDoctorsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.exception.ExceptionHandlerProcessor;
import uk.nhs.hee.tis.revalidation.integration.router.processor.AttachNotesToDoctorProcessor;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.MergeEnrichedDoctorsIntoSummaryProcessor;


@Component
public class RecommendationServiceRouter extends RouteBuilder {

  private final ExecutorService notesExecutor;
  @Value("${service.core.url}")
  private String coreServiceUrl;


  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private KeycloakBean keycloakBean;

  @Autowired
  private DoctorRecommendationAggregationStrategy doctorRecommendationAggregationStrategy;

  @Autowired
  private EnrichedDoctorsAggregationStrategy enrichedDoctorsAggregationStrategy;


  @Autowired
  private ExceptionHandlerProcessor exceptionHandlerProcessor;

  @Autowired
  private AttachNotesToDoctorProcessor attachNotesToDoctorProcessor;
  @Autowired
  private MergeEnrichedDoctorsIntoSummaryProcessor mergeEnrichedDoctorsIntoSummaryProcessor;


  @Value("${service.tcs.url}")
  private String tcsServiceUrl;

  @Value("${service.recommendation.url}")
  private String serviceUrl;

  public RecommendationServiceRouter(@Qualifier("notesExecutor") ExecutorService notesExecutor) {
    this.notesExecutor = notesExecutor;
  }

  @Override
  public void configure() {

    onException(HttpOperationFailedException.class)
        .process(exceptionHandlerProcessor);

    from("direct:tcs-trainees")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(tcsServiceUrl + "/api/revalidation/trainees/${header.gmcIds}?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson, Map.class);

    // TODO: Remove mapping when tis-revalidation-core is deployed.
    from("direct:temp-doctors-assign-admin")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .to(serviceUrl + "/api/v1/doctors/assign-admin?bridgeEndpoint=true");

    from("direct:recommendation-post")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + "/api/recommendation?bridgeEndpoint=true");

    from("direct:recommendation-put")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.PUT))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + "/api/recommendation?bridgeEndpoint=true");

    from("direct:recommendation-gmc-id")
        .to("direct:recommendation-trainee-by-gmc-id")
        .setHeader("gmcIds").simple("${header.gmcId}")
        .enrich("direct:tcs-trainees", doctorRecommendationAggregationStrategy);

    from("direct:recommendation-trainee-by-gmc-id")
        .toD(serviceUrl + "/api/recommendation/${header.gmcId}?bridgeEndpoint=true");

    from("direct:recommendation-submit")
        .to("direct:reval-officer")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + "/api/recommendation/${header.gmcId}/submit/${header.recommendationId}"
            + "?bridgeEndpoint=true");

    from("direct:admin")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .toD(serviceUrl + "/api/admin/trigger-doctor-sync?bridgeEndpoint=true");

    from("direct:recommendation-summary")
        .to(serviceUrl + "/api/v1/doctors?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson, TraineeSummaryDto.class)
        .to("direct:enrich-page-with-notes");

    from("direct:enrich-page-with-notes")
        .setProperty("summary", body())
        .split(simple("${exchangeProperty.summary.traineeInfo}"))
        .parallelProcessing()
        .executorService(notesExecutor)
        .stopOnException(false)
        .setProperty("doctor", body())
        .setHeader("gmcId", simple("${exchangeProperty.doctor.gmcReferenceNumber}"))
        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
        .toD(coreServiceUrl + "/api/trainee/${header.gmcId}/notes"
            + "?bridgeEndpoint=true"
            + "&throwExceptionOnFailure=false"
            + "&connectTimeout=1500"
            + "&socketTimeout=2500")
        .choice()
        .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
        .unmarshal().json(JsonLibrary.Jackson, TraineeNotesDto.class)
        .endChoice()
        .otherwise()
        .setBody(constant(null))
        .end()
        .process(attachNotesToDoctorProcessor)
        .end()
        .aggregate(constant(true), enrichedDoctorsAggregationStrategy)
        .completionPredicate(exchangeProperty(Exchange.SPLIT_COMPLETE).isEqualTo(true))
        .process(mergeEnrichedDoctorsIntoSummaryProcessor);

    from("direct:doctors-autocomplete")
        .to(serviceUrl + "/api/v1/doctors/autocomplete?bridgeEndpoint=true");
  }
}
