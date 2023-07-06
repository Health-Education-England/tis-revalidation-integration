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

package uk.nhs.hee.tis.revalidation.integration.router.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeRecommendationDto;

@Slf4j
@Component
public class DoctorRecommendationAggregationStrategy implements AggregationStrategy {

  private final ObjectMapper mapper;

  DoctorRecommendationAggregationStrategy(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @SneakyThrows
  @Override
  public Exchange aggregate(final Exchange oldExchange, final Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final InputStreamCache messageBody = (InputStreamCache) oldExchange.getIn().getBody();
    if (messageBody.length() == 0) {
      result.getIn().setBody(null);
      result.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.NOT_FOUND.value());
      return result;
    }

    final var traineeRecommendationDto = mapper.readValue(messageBody,
        TraineeRecommendationDto.class);
    final var traineeCoreDto = getTcsCoreRecord(newExchange,
        traineeRecommendationDto.getGmcNumber());
    if (traineeCoreDto != null) {
      traineeRecommendationDto.setCurrentGrade(traineeCoreDto.getCurrentGrade());
      traineeRecommendationDto
          .setProgrammeMembershipType(traineeCoreDto.getProgrammeMembershipType());
      traineeRecommendationDto.setCurriculumEndDate(traineeCoreDto.getCurriculumEndDate());
    }
    result.getMessage().setBody(traineeRecommendationDto);
    return result;
  }

  private TraineeCoreDto getTcsCoreRecord(final Exchange exchange, final String gmcId) {
    final var body = exchange.getIn().getBody();

    final var tcsCoreValue = (Map<String, String>) ((Map) body).get(gmcId);
    return tcsCoreValue != null ? mapper
        .convertValue(tcsCoreValue, TraineeCoreDto.class) : null;
  }
}
