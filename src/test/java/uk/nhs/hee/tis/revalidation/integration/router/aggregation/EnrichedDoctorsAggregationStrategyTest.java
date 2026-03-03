/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;

class EnrichedDoctorsAggregationStrategyTest {

  EnrichedDoctorsAggregationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new EnrichedDoctorsAggregationStrategy();
  }

  @Test
  void shouldAggregateDoctorsIntoListofTraineeInfoDto() {
    CamelContext context = new DefaultCamelContext();

    Exchange exchange1 = new DefaultExchange(context);
    TraineeInfoDto traineeInfoDto1 = new TraineeInfoDto();
    traineeInfoDto1.setGmcReferenceNumber("1");
    exchange1.getMessage().setBody(traineeInfoDto1);

    Exchange exchange2 = new DefaultExchange(context);
    TraineeInfoDto traineeInfoDto2 = new TraineeInfoDto();
    traineeInfoDto2.setGmcReferenceNumber("2");
    exchange2.getMessage().setBody(traineeInfoDto2);

    Exchange aggregatedExchange1 = strategy.aggregate(null, exchange1);
    Exchange aggregatedExchange2 = strategy.aggregate(aggregatedExchange1, exchange2);

    @SuppressWarnings("unchecked")
    List<TraineeInfoDto> list =
        (List<TraineeInfoDto>) aggregatedExchange2.getProperty(
            EnrichedDoctorsAggregationStrategy.ENRICHED_DOCTORS);

    assertNotNull(list);
    assertEquals(2, list.size());
    assertEquals("1", list.get(0).getGmcReferenceNumber());
    assertEquals("2", list.get(1).getGmcReferenceNumber());
  }
}
