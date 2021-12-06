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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeRecommendationDto;

class DoctorRecommendationAggregationStrategyTest {

  private static final String DESIGNATED_BODY = "abc-123";
  private static final String FULL_NAME = "Joe Bloggs";
  private static final String GMC_NUMBER = "gmc123";
  private static final String GMC_OUTCOME = "outcome2";
  private static final LocalDate GMC_SUBMISSION_DATE = LocalDate.EPOCH;
  private static final String PROGRAMME_NAME = "programme2";
  private static final String UNDER_NOTICE = "underNotice1";

  private static final LocalDate OLD_CCT_DATE = LocalDate.MIN;
  private static final String OLD_PROGRAMME_MEMBERSHIP_TYPE = "type1";
  private static final String OLD_GRADE = "grade1";

  private static final LocalDate NEW_CCT_DATE = LocalDate.MAX;
  private static final String NEW_PROGRAMME_MEMBERSHIP_TYPE = "type2";
  private static final String NEW_GRADE = "grade2";


  private DoctorRecommendationAggregationStrategy aggregationStrategy;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModules(new JavaTimeModule());
    aggregationStrategy = new DoctorRecommendationAggregationStrategy(objectMapper);
  }

  @Test
  void shouldAggregateRecommendationWhenCoreFound() {
    var camelContext = new DefaultCamelContext();

    var traineeRecommendation = new TraineeRecommendationDto();
    traineeRecommendation.setCctDate(OLD_CCT_DATE);
    traineeRecommendation.setCurrentGrade(OLD_GRADE);
    traineeRecommendation.setDesignatedBody(DESIGNATED_BODY);
    traineeRecommendation.setFullName(FULL_NAME);
    traineeRecommendation.setGmcNumber(GMC_NUMBER);
    traineeRecommendation.setGmcSubmissionDate(GMC_SUBMISSION_DATE);
    traineeRecommendation.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE);
    traineeRecommendation.setUnderNotice(UNDER_NOTICE);

    var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeRecommendation);
    var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    var traineeCore = new TraineeCoreDto();
    traineeCore.setCctDate(NEW_CCT_DATE);
    traineeCore.setCurrentGrade(NEW_GRADE);
    traineeCore.setGmcOutcome(GMC_OUTCOME);
    traineeCore.setProgrammeMembershipType(NEW_PROGRAMME_MEMBERSHIP_TYPE);
    traineeCore.setProgrammeName(PROGRAMME_NAME);

    var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(GMC_NUMBER, objectMapper.convertValue(traineeCore, Map.class)));
    var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(TraineeRecommendationDto.class));

    TraineeRecommendationDto aggregatedBody = aggregatedMessage
        .getBody(TraineeRecommendationDto.class);
    assertThat("Unexpected CCT date.", aggregatedBody.getCctDate(), is(NEW_CCT_DATE));
    assertThat("Unexpected current grade.", aggregatedBody.getCurrentGrade(), is(NEW_GRADE));
    assertThat("Unexpected designated body.", aggregatedBody.getDesignatedBody(),
        is(DESIGNATED_BODY));
    assertThat("Unexpected full name.", aggregatedBody.getFullName(), is(FULL_NAME));
    assertThat("Unexpected GMC number.", aggregatedBody.getGmcNumber(), is(GMC_NUMBER));
    assertThat("Unexpected GMC submission date.", aggregatedBody.getGmcSubmissionDate(),
        is(GMC_SUBMISSION_DATE));
    assertThat("Unexpected programme membership type.", aggregatedBody.getProgrammeMembershipType(),
        is(NEW_PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected under notice.", aggregatedBody.getUnderNotice(), is(UNDER_NOTICE));
  }

  @Test
  void shouldReturnUnchangedRecommendationWhenNoCoreFound() {
    var camelContext = new DefaultCamelContext();

    var traineeRecommendation = new TraineeRecommendationDto();
    traineeRecommendation.setCctDate(OLD_CCT_DATE);
    traineeRecommendation.setCurrentGrade(OLD_GRADE);
    traineeRecommendation.setDesignatedBody(DESIGNATED_BODY);
    traineeRecommendation.setFullName(FULL_NAME);
    traineeRecommendation.setGmcNumber(GMC_NUMBER);
    traineeRecommendation.setGmcSubmissionDate(GMC_SUBMISSION_DATE);
    traineeRecommendation.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE);
    traineeRecommendation.setUnderNotice(UNDER_NOTICE);

    var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeRecommendation);
    var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Collections.emptyMap());
    var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(TraineeRecommendationDto.class));

    TraineeRecommendationDto aggregatedBody = aggregatedMessage
        .getBody(TraineeRecommendationDto.class);
    assertThat("Unexpected CCT date.", aggregatedBody.getCctDate(), is(OLD_CCT_DATE));
    assertThat("Unexpected current grade.", aggregatedBody.getCurrentGrade(), is(OLD_GRADE));
    assertThat("Unexpected designated body.", aggregatedBody.getDesignatedBody(),
        is(DESIGNATED_BODY));
    assertThat("Unexpected full name.", aggregatedBody.getFullName(), is(FULL_NAME));
    assertThat("Unexpected GMC number.", aggregatedBody.getGmcNumber(), is(GMC_NUMBER));
    assertThat("Unexpected GMC submission date.", aggregatedBody.getGmcSubmissionDate(),
        is(GMC_SUBMISSION_DATE));
    assertThat("Unexpected programme membership type.", aggregatedBody.getProgrammeMembershipType(),
        is(OLD_PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected under notice.", aggregatedBody.getUnderNotice(), is(UNDER_NOTICE));
  }
}
