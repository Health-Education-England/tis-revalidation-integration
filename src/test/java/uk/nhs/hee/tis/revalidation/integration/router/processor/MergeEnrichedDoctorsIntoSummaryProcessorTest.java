/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.EnrichedDoctorsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

class MergeEnrichedDoctorsIntoSummaryProcessorTest {

  @Test
  void shouldMergeEnrichedListIntoSummary() {
    CamelContext context = new DefaultCamelContext();
    final Exchange exchange = new DefaultExchange(context);

    TraineeSummaryDto traineeSummaryDto = new TraineeSummaryDto();
    traineeSummaryDto.setTotalResults(2);

    TraineeInfoDto traineeInfoDto1 = new TraineeInfoDto();
    traineeInfoDto1.setGmcReferenceNumber("999999");

    TraineeInfoDto traineeInfoDto2 = new TraineeInfoDto();
    traineeInfoDto2.setGmcReferenceNumber("1000000");

    exchange.setProperty(MergeEnrichedDoctorsIntoSummaryProcessor.SUMMARY, traineeSummaryDto);
    exchange.setProperty(EnrichedDoctorsAggregationStrategy.ENRICHED_DOCTORS,
        List.of(traineeInfoDto1, traineeInfoDto2));

    MergeEnrichedDoctorsIntoSummaryProcessor testObj =
        new MergeEnrichedDoctorsIntoSummaryProcessor();
    testObj.process(exchange);

    TraineeSummaryDto out = exchange.getMessage().getBody(TraineeSummaryDto.class);
    assertNotNull(out);
    assertNotNull(out.getTraineeInfo());
    assertEquals(2, out.getTraineeInfo().size());
    assertEquals("999999", out.getTraineeInfo().get(0).getGmcReferenceNumber());
    assertEquals("1000000", out.getTraineeInfo().get(1).getGmcReferenceNumber());
  }
}
