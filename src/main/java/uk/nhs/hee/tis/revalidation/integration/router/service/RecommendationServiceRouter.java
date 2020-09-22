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
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorRecommendationAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorRecommendationSummaryAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;

@Component
public class RecommendationServiceRouter extends RouteBuilder {

  private static final String API_RECOMMENDATION = "/api/recommendation?bridgeEndpoint=true";
  private static final String API_RECOMMENDATION_GMC_ID =
      "/api/recommendation/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_RECOMMENDATION_SUBMIT =
      "/api/recommendation/${header.gmcId}/submit/${header.recommendationId}?bridgeEndpoint=true";
  private static final String API_CONNECTION = "/api/revalidation/trainees/${header.gmcIds}?bridgeEndpoint=true";

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private DoctorRecommendationSummaryAggregationStrategy doctorRecommendationSummaryAggregationStrategy;

  @Autowired
  private DoctorRecommendationAggregationStrategy doctorRecommendationAggregationStrategy;

  @Value("${service.tcs.url}")
  private String tcsServiceUrl;

  @Value("${service.recommendation.url}")
  private String serviceUrl;

  @Override
  public void configure() {

    // TODO: Remove mapping when tis-revalidation-core is deployed.
    from("direct:temp-doctors")
        .to("direct:v1-doctors")
        .setHeader("gmcIds").method(gmcIdProcessorBean, "process")
        .enrich("direct:tcs-trainees", doctorRecommendationSummaryAggregationStrategy);

    from("direct:tcs-trainees")
        .toD(tcsServiceUrl + API_CONNECTION)
        .unmarshal().json(JsonLibrary.Jackson, Map.class);

    // TODO: Remove mapping when tis-revalidation-core is deployed.
    from("direct:temp-doctors-assign-admin")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .to(serviceUrl + "/api/v1/doctors/assign-admin?bridgeEndpoint=true");

    from("direct:recommendation-post")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + API_RECOMMENDATION);

    from("direct:recommendation-put")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.PUT))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + API_RECOMMENDATION);

    from("direct:recommendation-gmc-id")
        .to("direct:recommendation-trainee-by-gmc-id")
        .setHeader("gmcIds").method(gmcIdProcessorBean, "getGmcIdOfRecommendationTrainee")
        .enrich("direct:tcs-trainees", doctorRecommendationAggregationStrategy);

    from("direct:recommendation-trainee-by-gmc-id")
        .toD(serviceUrl + API_RECOMMENDATION_GMC_ID)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:recommendation-submit")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .toD(serviceUrl + API_RECOMMENDATION_SUBMIT);
  }
}
