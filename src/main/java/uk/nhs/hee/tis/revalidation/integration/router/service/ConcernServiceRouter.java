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

import org.apache.camel.AggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.AggregationKey;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.JsonStringAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class ConcernServiceRouter extends RouteBuilder {

  private static final int MAX_RECORD_SHOWN = 5;

  private static final String API_CONCERNS = "/api/concerns?bridgeEndpoint=true";
  private static final String API_CONCERN_ADMINS = "/api/concerns/admins?bridgeEndpoint=true";
  private static final String API_CONCERNS_GMC_ID =
      "/api/concerns/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_SITES = "/api/sites?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";
  private static final String API_GRADES = "/api/current/grades?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";
  private static final String API_TRUSTS = "/api/trusts?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";

  private static final String OIDC_ACCESS_TOKEN_HEADER = "OIDC_access_token";
  private static final String GET_TOKEN_METHOD = "getAuthToken";
  private static final AggregationStrategy AGGREGATOR = new JsonStringAggregationStrategy();

  @Autowired
  private KeycloakBean reference;

  @Value("${service.concern.url}")
  private String serviceUrlConcern;

  @Value("${service.reference.url}")
  private String serviceUrlReference;

  @Override
  public void configure() throws Exception {

    from("direct:concerns")
        .to(serviceUrlConcern + API_CONCERNS)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:concern-admins")
        .to(serviceUrlConcern + API_CONCERN_ADMINS)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:concerns-gmc-id-aggregation")
        .multicast(AGGREGATOR)
        .parallelProcessing()
        .to("direct:concerns-gmc-id")
        .to("direct:reference-sites")
        .to("direct:reference-grades")
        .to("direct:reference-employers");
    from("direct:concerns-gmc-id")
        .toD(serviceUrlConcern + API_CONCERNS_GMC_ID)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.CONCERNS);
    from("direct:reference-sites")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(reference, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.SITES)
        .toD(serviceUrlReference + API_SITES);
    from("direct:reference-grades")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(reference, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.GRADES)
        .toD(serviceUrlReference + API_GRADES);
    from("direct:reference-employers")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(reference, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.EMPLOYERS)
        .toD(serviceUrlReference + API_TRUSTS);


  }
}
