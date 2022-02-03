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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationTcsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.RecommendationOutcomeMapper;

class RecommendationTcsAggregationStrategyTest {

  private static final String KEY_1 = "identifier1";
  private static final String KEY_2 = "identifier2";

  private static final LocalDate OLD_CURRICULUM_END_DATE_1 = LocalDate.now();
  private static final String OLD_CURRENT_GRADE_1 = "grade1";
  private static final String OLD_GMC_OUTCOME_1 = "outcome1";
  private static final String OLD_PROGRAMME_MEMBERSHIP_TYPE_1 = "type1";
  private static final String OLD_PROGRAMME_NAME_1 = "programme1";

  private static final LocalDate OLD_CURRICULUM_END_DATE_2 = LocalDate.now().plusYears(1);
  private static final String OLD_CURRENT_GRADE_2 = "grade2";
  private static final String OLD_GMC_OUTCOME_2 = "outcome2";
  private static final String OLD_PROGRAMME_MEMBERSHIP_TYPE_2 = "type2";
  private static final String OLD_PROGRAMME_NAME_2 = "programme2";

  private static final LocalDate NEW_CURRICULUM_END_DATE_1 = LocalDate.now().plusYears(2);
  private static final String NEW_CURRENT_GRADE_1 = "grade10";
  private static final String NEW_PROGRAMME_MEMBERSHIP_TYPE_1 = "type10";
  private static final String NEW_PROGRAMME_NAME_1 = "programme10";

  private static final LocalDate NEW_CURRICULUM_END_DATE_2 = LocalDate.now().plusYears(3);
  private static final String NEW_CURRENT_GRADE_2 = "grade20";
  private static final String NEW_PROGRAMME_MEMBERSHIP_TYPE_2 = "type20";
  private static final String NEW_PROGRAMME_NAME_2 = "programme20";

  private RecommendationTcsAggregationStrategy aggregationStrategy;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModules(new JavaTimeModule());
    RecommendationOutcomeMapper outcomeMapper = Mappers
        .getMapper(RecommendationOutcomeMapper.class);
    aggregationStrategy = new RecommendationTcsAggregationStrategy(objectMapper, outcomeMapper);
  }

  @Test
  void shouldAggregateTraineeCoresWhenValid() throws JsonProcessingException {
    var traineeCore1 = new TraineeCoreDto();
    traineeCore1.setCurriculumEndDate(OLD_CURRICULUM_END_DATE_1);
    traineeCore1.setCurrentGrade(OLD_CURRENT_GRADE_1);
    traineeCore1.setGmcOutcome(OLD_GMC_OUTCOME_1);
    traineeCore1.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE_1);
    traineeCore1.setProgrammeName(OLD_PROGRAMME_NAME_1);
    var traineeCore2 = new TraineeCoreDto();
    traineeCore2.setCurriculumEndDate(OLD_CURRICULUM_END_DATE_2);
    traineeCore2.setCurrentGrade(OLD_CURRENT_GRADE_2);
    traineeCore2.setGmcOutcome(OLD_GMC_OUTCOME_2);
    traineeCore2.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE_2);
    traineeCore2.setProgrammeName(OLD_PROGRAMME_NAME_2);

    var camelContext = new DefaultCamelContext();
    var oldMessage = new DefaultMessage(camelContext);
    oldMessage
        .setBody(objectMapper.writeValueAsString(Map.of(KEY_1, traineeCore1, KEY_2, traineeCore2)));
    var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    var recommendation1 = new RecommendationTcsDto();
    recommendation1.setCurriculumEndDate(NEW_CURRICULUM_END_DATE_1);
    recommendation1.setCurrentGrade(NEW_CURRENT_GRADE_1);
    recommendation1.setProgrammeMembershipType(NEW_PROGRAMME_MEMBERSHIP_TYPE_1);
    recommendation1.setProgrammeName(NEW_PROGRAMME_NAME_1);
    var recommendation2 = new RecommendationTcsDto();
    recommendation2.setCurriculumEndDate(NEW_CURRICULUM_END_DATE_2);
    recommendation2.setCurrentGrade(NEW_CURRENT_GRADE_2);
    recommendation2.setProgrammeMembershipType(NEW_PROGRAMME_MEMBERSHIP_TYPE_2);
    recommendation2.setProgrammeName(NEW_PROGRAMME_NAME_2);

    var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(KEY_1, recommendation1, KEY_2, recommendation2));
    var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(), instanceOf(Map.class));

    Map<String, TraineeCoreDto> aggregatedTrainees = aggregatedMessage.getBody(Map.class);
    assertThat("Unexpected number of trainee records.", aggregatedTrainees.size(), is(2));
    assertThat("Unexpected trainee identifiers.", aggregatedTrainees.keySet(),
        hasItems(KEY_1, KEY_2));

    TraineeCoreDto aggregatedTrainee = aggregatedTrainees.get(KEY_1);
    assertThat("Unexpected curriculumEndDate date.",
        aggregatedTrainee.getCurriculumEndDate(), is(NEW_CURRICULUM_END_DATE_1));
    assertThat("Unexpected current grade.", aggregatedTrainee.getCurrentGrade(),
        is(NEW_CURRENT_GRADE_1));
    assertThat("Unexpected GMC outcome.", aggregatedTrainee.getGmcOutcome(), is(OLD_GMC_OUTCOME_1));
    assertThat("Unexpected programme membership type.",
        aggregatedTrainee.getProgrammeMembershipType(), is(NEW_PROGRAMME_MEMBERSHIP_TYPE_1));
    assertThat("Unexpected programme name.", aggregatedTrainee.getProgrammeName(),
        is(NEW_PROGRAMME_NAME_1));

    aggregatedTrainee = aggregatedTrainees.get(KEY_2);
    assertThat("Unexpected curriculumEndDate date.",
        aggregatedTrainee.getCurriculumEndDate(), is(NEW_CURRICULUM_END_DATE_2));
    assertThat("Unexpected current grade.", aggregatedTrainee.getCurrentGrade(),
        is(NEW_CURRENT_GRADE_2));
    assertThat("Unexpected GMC outcome.", aggregatedTrainee.getGmcOutcome(), is(OLD_GMC_OUTCOME_2));
    assertThat("Unexpected programme membership type.",
        aggregatedTrainee.getProgrammeMembershipType(), is(NEW_PROGRAMME_MEMBERSHIP_TYPE_2));
    assertThat("Unexpected programme name.", aggregatedTrainee.getProgrammeName(),
        is(NEW_PROGRAMME_NAME_2));
  }

  @Test
  void shouldReturnNullWhenInvalid() {
    var camelContext = new DefaultCamelContext();

    Exchange exchange = aggregationStrategy
        .aggregate(new DefaultExchange(camelContext), new DefaultExchange(camelContext));

    assertThat("Unexpected exchange.", exchange, nullValue());
  }
}
