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

import static java.util.stream.Collectors.toList;
import static org.mapstruct.factory.Mappers.getMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.RecommendationSummaryMapper;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.TraineeRecommendationMapper;

@Slf4j
@Component
public class DoctorRecommendationSummaryAggregationStrategy implements AggregationStrategy {

  @Autowired
  private ObjectMapper mapper;

  @SneakyThrows
  @Override
  public Exchange aggregate(final Exchange oldExchange, final Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final var messageBody = oldExchange.getIn().getBody();
    final var traineeSummaryDto = mapper.convertValue(messageBody, TraineeSummaryDto.class);
    final var traineeInfos = traineeSummaryDto.getTraineeInfo();

    final var recommendationInfoDtos = traineeInfos.stream().map(traineeInfo -> {
      return aggregateTraineeWithRecommendation(newExchange, traineeInfo);
    }).collect(toList());

    final var recommendationSummaryMapper = getMapper(RecommendationSummaryMapper.class);
    final var recommendationSummaryDto = recommendationSummaryMapper
        .mergeConnectionInfo(traineeSummaryDto, recommendationInfoDtos);

    result.getMessage().setBody(recommendationSummaryDto);
    return result;
  }

  private RecommendationInfoDto aggregateTraineeWithRecommendation(final Exchange newExchange,
      final TraineeInfoDto traineeInfo) {

    final var traineeCoreDto = getTcsCoreRecord(newExchange,
        traineeInfo.getGmcReferenceNumber());
    final var mapper = getMapper(TraineeRecommendationMapper.class);
    return mapper.mergeTraineeRecommendationResponses(traineeInfo, traineeCoreDto);
  }

  private TraineeCoreDto getTcsCoreRecord(final Exchange exchange, final String gmcId) {
    final var body = exchange.getIn().getBody();

    final var tcsCoreValue = ((Map) body).get(gmcId);
    return tcsCoreValue != null ? mapper
        .convertValue(tcsCoreValue, TraineeCoreDto.class) : null;
  }
}
