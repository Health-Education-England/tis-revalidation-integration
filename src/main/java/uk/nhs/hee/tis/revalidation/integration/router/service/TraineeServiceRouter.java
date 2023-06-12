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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.TraineeNotesAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.TraineeTcsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;
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
      "/api/trainee/notes/add?bridgeEndpoint=true";
  private static final String API_TRAINEEENOTES_EDIT =
      "/api/trainee/notes/edit?bridgeEndpoint=true";
  private static final String API_REVAL_TRAINEE =
      "/api/v1/doctors/gmcIds/${header.gmcIds}?bridgeEndpoint=true";

  @Autowired
  private KeycloakBean keycloakBean;

  @Autowired
  private GmcIdProcessorBean gmcIdProcessorBean;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private TraineeNotesAggregationStrategy traineeNotesAggregationStrategy;

  @Autowired
  private TraineeTcsAggregationStrategy traineeTcsAggregationStrategy;

  @Value("${service.tcs.url}")
  private String serviceUrl;

  @Value("${service.core.url}")
  private String coreServiceUrl;

  @Value("${service.recommendation.url}")
  private String recommendationServiceUrl;

  @Override
  public void configure() {

    from("direct:trainee")
        // I'm thinking of sending a request to Recommendation to check if the gmc exists in DoctorsForDB first
        // below 2 lines doesn't work, think I need to find a way to check if the countTotal in the body equals to 0
        .to("direct:reval-trainee-details")
        .choice().when(exchange -> mapper.convertValue(exchange.getIn().getBody(), TraineeSummaryDto.class).getCountTotal() == 0).endChoice()
            .when(exchange -> mapper.convertValue(exchange.getIn().getBody(), TraineeSummaryDto.class).getCountTotal() == 1)
            .enrich("direct:trainee-details", traineeTcsAggregationStrategy)
            .enrich("direct:traineenotes-get", traineeNotesAggregationStrategy)
            .endChoice();

    // new route for getting doctor from Reval DoctorsForDB
    from("direct:reval-trainee-details")
        .setHeader("gmcIds").simple("${header.gmcId}")
        .toD(recommendationServiceUrl + API_REVAL_TRAINEE)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:trainee-details")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_TRAINEE + "&throwExceptionOnFailure=false")
        .unmarshal().json(JsonLibrary.Jackson);
//        .choice()
//          .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(HttpStatus.OK))
////            .unmarshal().json(JsonLibrary.Jackson)
//          .endChoice()
//          .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(HttpStatus.NOT_FOUND))
//              .setBody(exchange -> TraineeDetailsDto.builder())
////          .unmarshal().json(JsonLibrary.Jackson)
//          .endChoice();

    from("direct:traineenotes-get")
        .toD(coreServiceUrl + API_TRAINEENOTES)
        .unmarshal().json(JsonLibrary.Jackson);
    from("direct:traineenotes-add")
        .to(coreServiceUrl + API_TRAINEEENOTES_ADD);
    from("direct:traineenotes-edit")
        .to(coreServiceUrl + API_TRAINEEENOTES_EDIT);
    from("direct:trainees")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_TRAINEES);
  }
}
