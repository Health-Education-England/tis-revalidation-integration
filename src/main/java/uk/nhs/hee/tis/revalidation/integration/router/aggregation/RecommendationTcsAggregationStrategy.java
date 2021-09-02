/*
 * The MIT License (MIT)
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
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationTcsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.RecommendationOutcomeMapper;

@Slf4j
@Component
public class RecommendationTcsAggregationStrategy implements AggregationStrategy {

  @Autowired
  private ObjectMapper mapper;

  @SneakyThrows
  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    try {
      final var messageBody = oldExchange.getIn().getBody(String.class);
      final Map<String, TraineeCoreDto> recommendationRecordMap = mapper
          .readValue(messageBody, Map.class);
      final Map<String, RecommendationTcsDto> recommendationTcsMap = newExchange.getIn()
          .getBody(Map.class);

      final var recommendationOutcomeMapper = getMapper(RecommendationOutcomeMapper.class);
      final var keys = recommendationRecordMap.keySet();
      keys.stream().forEach(key -> {
        final var recommendationInfo = mapper
            .convertValue(recommendationRecordMap.get(key), TraineeCoreDto.class);
        final var recommendationTcs = mapper
            .convertValue(recommendationTcsMap.get(key), RecommendationTcsDto.class);
        recommendationRecordMap
            .put(key, recommendationOutcomeMapper
                .mergeRecommendationOutcomeResponses(recommendationInfo, recommendationTcs));
      });

      result.getMessage().setBody(recommendationRecordMap);
      return result;
    } catch (Exception e) {
      log.info("Exception in RecommendationTcsAggregationStrategy", e);
      return null;
    }
  }
}
