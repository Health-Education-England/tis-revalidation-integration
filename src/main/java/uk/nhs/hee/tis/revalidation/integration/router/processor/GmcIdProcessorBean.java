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

package uk.nhs.hee.tis.revalidation.integration.router.processor;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ExceptionResponseDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class GmcIdProcessorBean {

  @Autowired
  private ObjectMapper mapper;

  public List<String> process(final Exchange exchange) throws JsonProcessingException {
    final var body = exchange.getIn().getBody();
    final var traineeSummaryDto = mapper.convertValue(body, TraineeSummaryDto.class);
    if (traineeSummaryDto.getTraineeInfo() != null) {
      return traineeSummaryDto.getTraineeInfo().stream().map(t -> t.getGmcReferenceNumber())
          .collect(toList());
    }
    return List.of();

  }

  public String getGmcIdOfRecommendationTrainee(final Exchange exchange)
      throws JsonProcessingException {
    final var body = exchange.getIn().getBody();
    final var traineeRecommendationDto = mapper.convertValue(body, TraineeRecommendationDto.class);
    return traineeRecommendationDto.getGmcNumber();
  }

  public List<String> getConnectionExceptionGmcIds(final Exchange exchange)
      throws JsonProcessingException {
    final var body = exchange.getIn().getBody();
    final var exceptionResponseDto = mapper.convertValue(body, ExceptionResponseDto.class);
    return exceptionResponseDto.getExceptionRecord().stream().map(e -> e.getGmcId())
        .collect(toList());
  }

  public List<String> getHiddenGmcIds(final Exchange exchange)
      throws JsonProcessingException {
    final var body = exchange.getIn().getBody(String.class);
    List<String> gmcIds = mapper.readValue(body, new TypeReference<List<String>>(){});
    return gmcIds;
  }
}
