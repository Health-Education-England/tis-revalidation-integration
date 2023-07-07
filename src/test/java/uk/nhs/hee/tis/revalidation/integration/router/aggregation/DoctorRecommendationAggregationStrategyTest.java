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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
  private static final LocalDate OLD_CURRICULUM_END_DATE = LocalDate.MIN;
  private static final String OLD_PROGRAMME_MEMBERSHIP_TYPE = "type1";
  private static final String OLD_GRADE = "grade1";
  private static final LocalDate NEW_CURRICULUM_END_DATE = LocalDate.MAX;
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
  void shouldReturn404WhenDoctorNotFound() {
    final var camelContext = new DefaultCamelContext();

    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(new InputStreamCache(new byte[]{}));

    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var newExchange = new DefaultExchange(camelContext);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    assertThat(aggregatedExchange.getIn().getBody(), nullValue());
    assertThat(aggregatedExchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE),
        equalTo(HttpStatus.NOT_FOUND.value()));
  }

  @Test
  void shouldAggregateRecommendationWhenCoreFound() throws IOException {
    final var camelContext = new DefaultCamelContext();

    final var traineeRecommendation = new TraineeRecommendationDto();
    traineeRecommendation.setCurriculumEndDate(OLD_CURRICULUM_END_DATE);
    traineeRecommendation.setCurrentGrade(OLD_GRADE);
    traineeRecommendation.setDesignatedBody(DESIGNATED_BODY);
    traineeRecommendation.setFullName(FULL_NAME);
    traineeRecommendation.setGmcNumber(GMC_NUMBER);
    traineeRecommendation.setGmcSubmissionDate(GMC_SUBMISSION_DATE);
    traineeRecommendation.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE);
    traineeRecommendation.setUnderNotice(UNDER_NOTICE);

    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(new InputStreamCache(objectMapper.writeValueAsBytes(traineeRecommendation)));

    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var traineeCore = new TraineeCoreDto();
    traineeCore.setCurriculumEndDate(NEW_CURRICULUM_END_DATE);
    traineeCore.setCurrentGrade(NEW_GRADE);
    traineeCore.setGmcOutcome(GMC_OUTCOME);
    traineeCore.setProgrammeMembershipType(NEW_PROGRAMME_MEMBERSHIP_TYPE);
    traineeCore.setProgrammeName(PROGRAMME_NAME);

    final var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(GMC_NUMBER, objectMapper.convertValue(traineeCore, Map.class)));
    final var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    final Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(TraineeRecommendationDto.class));

    final TraineeRecommendationDto aggregatedBody = aggregatedMessage
        .getBody(TraineeRecommendationDto.class);
    assertThat("Unexpected curriculumEndDate date.", aggregatedBody.getCurriculumEndDate(),
        is(NEW_CURRICULUM_END_DATE));
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
  void shouldReturnUnchangedRecommendationWhenNoCoreFound() throws IOException {
    final var camelContext = new DefaultCamelContext();

    final var traineeRecommendation = new TraineeRecommendationDto();
    traineeRecommendation.setCurriculumEndDate(OLD_CURRICULUM_END_DATE);
    traineeRecommendation.setCurrentGrade(OLD_GRADE);
    traineeRecommendation.setDesignatedBody(DESIGNATED_BODY);
    traineeRecommendation.setFullName(FULL_NAME);
    traineeRecommendation.setGmcNumber(GMC_NUMBER);
    traineeRecommendation.setGmcSubmissionDate(GMC_SUBMISSION_DATE);
    traineeRecommendation.setProgrammeMembershipType(OLD_PROGRAMME_MEMBERSHIP_TYPE);
    traineeRecommendation.setUnderNotice(UNDER_NOTICE);

    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(new InputStreamCache(objectMapper.writeValueAsBytes(traineeRecommendation)));
    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Collections.emptyMap());
    final var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    final Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(TraineeRecommendationDto.class));

    final TraineeRecommendationDto aggregatedBody = aggregatedMessage
        .getBody(TraineeRecommendationDto.class);
    assertThat("Unexpected curriculumEndDate date.", aggregatedBody.getCurriculumEndDate(),
        is(OLD_CURRICULUM_END_DATE));
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
