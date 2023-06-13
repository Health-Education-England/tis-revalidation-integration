/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class TraineeTcsAggregationStrategy implements AggregationStrategy {

  private final ObjectMapper mapper;

  TraineeTcsAggregationStrategy(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @SneakyThrows
  @Override
  public Exchange aggregate(final Exchange oldExchange, final Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final var oldMessageBody = oldExchange.getIn().getBody();
    final TraineeSummaryDto traineeSummeryDto = mapper
        .convertValue(oldMessageBody, TraineeSummaryDto.class);
    final TraineeInfoDto traineeInfoDto = traineeSummeryDto.getTraineeInfo().get(0);


    final var newMessageBody = newExchange.getIn().getBody();
    final TraineeDetailsDto traineeDetailsDto = mapper.convertValue(newMessageBody, TraineeDetailsDto.class);

    TraineeDetailsDto returnDto = traineeDetailsDto;
    // If doctor doesn't exist in TIS
    if (returnDto.getGmcNumber() == null) {
      returnDto.setGmcNumber(traineeInfoDto.getGmcReferenceNumber());
      returnDto.setForenames(traineeInfoDto.getDoctorFirstName());
      returnDto.setSurname(traineeInfoDto.getDoctorLastName());
    }
    result.getMessage().setHeaders(newExchange.getMessage().getHeaders());
    result.getMessage().setBody(returnDto);
    return result;
  }
}
