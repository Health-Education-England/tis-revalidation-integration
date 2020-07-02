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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RecommendationServiceRouter extends RouteBuilder {

  private static final String API_RECOMMENDATION = "/api/recommendation";

  @Value("${service.recommendation.url}")
  private String serviceUrl;

  @Override
  public void configure() {
    restConfiguration()
        .component("servlet")
        .bindingMode(RestBindingMode.auto);

    rest(API_RECOMMENDATION)
        .post()
        .route()
        .marshal().json(JsonLibrary.Jackson)
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:recommendation");

    rest(API_RECOMMENDATION)
        .put()
        .route()
        .marshal().json(JsonLibrary.Jackson)
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:recommendation");

    rest(API_RECOMMENDATION)
        .get("/{gmcId}")
        .toD("direct:recommendation-gmc-id");

    rest(API_RECOMMENDATION)
        .post("/{gmcId}/submit/{recommendationId}")
        .route()
        .marshal().json(JsonLibrary.Jackson)
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:recommendation-submit");

    from("direct:recommendation")
        .to(serviceUrl + API_RECOMMENDATION + "?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:recommendation-gmc-id")
        .toD(serviceUrl + API_RECOMMENDATION + "/${header.gmcId}?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:recommendation-submit")
        .toD(serviceUrl + API_RECOMMENDATION + "/${header.gmcId}/submit/${header.recommendationId}?bridgeEndpoint=true")
        .unmarshal().json(JsonLibrary.Jackson);
  }
}
