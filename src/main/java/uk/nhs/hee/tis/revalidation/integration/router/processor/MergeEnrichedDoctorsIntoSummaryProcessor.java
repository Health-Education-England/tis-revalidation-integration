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

import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.EnrichedDoctorsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

/**
 * Merging logic class for doctor notes enrichment.
 */
@Component
public class MergeEnrichedDoctorsIntoSummaryProcessor implements Processor {

  public static final String SUMMARY = "summary";

  @Override
  public void process(Exchange exchange) {
    TraineeSummaryDto summary = exchange.getProperty(SUMMARY, TraineeSummaryDto.class);

    @SuppressWarnings("unchecked")
    List<TraineeInfoDto> enriched =
        (List<TraineeInfoDto>) exchange.getProperty(
            EnrichedDoctorsAggregationStrategy.ENRICHED_DOCTORS);

    summary.setTraineeInfo(enriched);

    exchange.getMessage().setBody(summary, TraineeSummaryDto.class);
  }
}
