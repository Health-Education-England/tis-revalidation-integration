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
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.ConcernTcsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.DoctorConcernAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.JsonStringAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class ConcernServiceRouter extends RouteBuilder {

  private static final int MAX_RECORD_SHOWN = 5;

  private static final String API_CONCERNS = "/api/concerns?bridgeEndpoint=true";
  private static final String API_CONCERN_ADMINS = "/api/concerns/admins?bridgeEndpoint=true";
  private static final String API_CONCERNS_GMC_ID =
      "/api/concerns/${header.gmcId}?bridgeEndpoint=true";
  private static final String API_SITES =
      "/api/sites?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";
  private static final String API_GRADES =
      "/api/current/grades?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";
  private static final String API_TRUSTS =
      "/api/trusts?size=" + MAX_RECORD_SHOWN + "&bridgeEndpoint=true";
  private static final String API_SOURCES = "/api/sources?bridgeEndpoint=true";
  private static final String API_TYPES = "/api/concern-types?bridgeEndpoint=true";
  private static final String API_LATEST_CONCERNS = "/api/concerns/summary/${header.gmcIds}?bridgeEndpoint=true";

  private static final AggregationStrategy AGGREGATOR = new JsonStringAggregationStrategy();

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private DoctorConcernAggregationStrategy doctorConcernAggregationStrategy;

  @Autowired
  private ConcernTcsAggregationStrategy concernTcsAggregationStrategy;

  @Autowired
  private KeycloakBean reference;

  @Value("${service.concern.url}")
  private String serviceUrlConcern;

  @Value("${service.reference.url}")
  private String serviceUrlReference;

  @Override
  public void configure() throws Exception {

    from("direct:concerns-summary")
        .to("direct:v1-doctors")
        .setHeader("gmcIds").method(gmcIdProcessorBean, "process")
        .enrich("direct:latest-concern", doctorConcernAggregationStrategy);

    from("direct:latest-concern")
        .toD(serviceUrlConcern + API_LATEST_CONCERNS)
        .enrich("direct:tcs-trainees", concernTcsAggregationStrategy);

    from("direct:concern-save")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON))
        .to(serviceUrlConcern + API_CONCERNS);

    from("direct:concern-admins")
        .to(serviceUrlConcern + API_CONCERN_ADMINS)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:concerns-gmc-id-aggregation")
        .multicast(AGGREGATOR)
        .parallelProcessing()
        .to("direct:gmc-number")
        .to("direct:concerns-gmc-id")
        .to("direct:reference-sites")
        .to("direct:reference-grades")
        .to("direct:reference-employers")
        .to("direct:reference-sources")
        .to("direct:reference-types");
    from("direct:gmc-number")
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.GMC_NUMBER)
        .setBody().simple("${header.gmcId}");
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
    from("direct:reference-sources")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(reference, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.SOURCES)
        .toD(serviceUrlReference + API_SOURCES);
    from("direct:reference-types")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(reference, GET_TOKEN_METHOD)
        .setHeader(AggregationKey.HEADER).constant(AggregationKey.TYPES)
        .toD(serviceUrlReference + API_TYPES);
  }
}
