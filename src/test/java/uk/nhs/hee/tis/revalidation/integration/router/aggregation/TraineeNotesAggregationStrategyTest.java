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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesInfoDto;

class TraineeNotesAggregationStrategyTest {

  private TraineeNotesAggregationStrategy aggregationStrategy;

  @BeforeEach
  void setUp() {
    aggregationStrategy = new TraineeNotesAggregationStrategy(new ObjectMapper());
  }

  @Test
  void shouldAddNotesToTraineeDetails() {
    var camelContext = new DefaultCamelContext();

    var traineeDetails = new TraineeDetailsDto();
    traineeDetails.setTisPersonId(40);
    traineeDetails.setGmcNumber("gmc1");
    traineeDetails.setForenames("forename");
    traineeDetails.setSurname("surname");
    traineeDetails.setNotes(null);

    var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeDetails);
    var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    var traineeNotesInfo1 = new TraineeNotesInfoDto();
    traineeNotesInfo1.setId("note1");
    traineeNotesInfo1.setText("note text");
    var traineeNotesInfo2 = new TraineeNotesInfoDto();
    traineeNotesInfo2.setId("note2");
    traineeNotesInfo2.setText("note text");

    var traineeNotes = new TraineeNotesDto();
    traineeNotes.setGmcId("gmc2");
    traineeNotes.setNotes(List.of(traineeNotesInfo1, traineeNotesInfo2));

    var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(traineeNotes);
    var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);

    TraineeDetailsDto aggregatedRecord = (TraineeDetailsDto) aggregatedExchange.getMessage()
        .getBody();
    assertThat("Unexpected TIS ID.", aggregatedRecord.getTisPersonId(), is(40));
    assertThat("Unexpected GMC number.", aggregatedRecord.getGmcNumber(), is("gmc1"));
    assertThat("Unexpected forenames.", aggregatedRecord.getForenames(), is("forename"));
    assertThat("Unexpected surname.", aggregatedRecord.getSurname(), is("surname"));
    assertThat("Unexpected number of notes.", aggregatedRecord.getNotes().size(), is(2));
    assertThat("Unexpected notes.", aggregatedRecord.getNotes(),
        hasItems(traineeNotesInfo1, traineeNotesInfo2));
  }
}
