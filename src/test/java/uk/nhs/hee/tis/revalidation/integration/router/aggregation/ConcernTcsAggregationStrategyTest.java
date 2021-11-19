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

package uk.nhs.hee.tis.revalidation.integration.router.aggregation;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConcernRecordDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;

class ConcernTcsAggregationStrategyTest {

  private static final String KEY_1 = "identifier1";
  private static final String KEY_2 = "identifier2";

  private static final String OLD_PROGRAMME_1 = "programme1";
  private static final String OLD_PROGRAMME_2 = "programme2";
  private static final String NEW_PROGRAMME_1 = "programme10";
  private static final String NEW_PROGRAMME_2 = "programme20";

  private ConcernTcsAggregationStrategy aggregationStrategy;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    aggregationStrategy = new ConcernTcsAggregationStrategy(new ObjectMapper());
  }

  @Test
  void shouldUpdateProgrammeName() throws JsonProcessingException {
    var camelContext = new DefaultCamelContext();

    var concernRecord1 = new ConcernRecordDto();
    concernRecord1.setProgramme(OLD_PROGRAMME_1);
    var concernRecord2 = new ConcernRecordDto();
    concernRecord1.setProgramme(OLD_PROGRAMME_2);
    Map<String, ConcernRecordDto> oldBody = Map.of(KEY_1, concernRecord1, KEY_2, concernRecord2);

    var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(objectMapper.writeValueAsString(oldBody));
    var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    var traineeCore1 = new TraineeCoreDto();
    traineeCore1.setProgrammeName(NEW_PROGRAMME_1);
    var traineeCore2 = new TraineeCoreDto();
    traineeCore2.setProgrammeName(NEW_PROGRAMME_2);
    Map<String, TraineeCoreDto> newBody = Map.of(KEY_1, traineeCore1, KEY_2, traineeCore2);

    var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(newBody);
    var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    String aggregatedBody = aggregatedExchange.getMessage().getBody(String.class);
    Map<String, ConcernRecordDto> aggregatedMap = objectMapper.readValue(aggregatedBody, Map.class);

    assertThat("Unexpected number of records.", aggregatedMap.size(), is(2));
    assertThat("Unexpected record identifiers.", aggregatedMap.keySet(), hasItems(KEY_1, KEY_2));

    ConcernRecordDto aggregatedRecord = objectMapper
        .convertValue(aggregatedMap.get(KEY_1), ConcernRecordDto.class);
    assertThat("Unexpected programme name.", aggregatedRecord.getProgramme(), is(NEW_PROGRAMME_1));
    aggregatedRecord = objectMapper.convertValue(aggregatedMap.get(KEY_2), ConcernRecordDto.class);
    assertThat("Unexpected programme name.", aggregatedRecord.getProgramme(), is(NEW_PROGRAMME_2));
  }
}
