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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RecommendationSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.RecommendationSummaryMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.TraineeRecommendationMapperImpl;

class DoctorRecommendationSummaryAggregationStrategyTest {

  private static final String ADMIN_1 = "admin1";
  private static final String ADMIN_2 = "admin2";
  private static final LocalDate CURRICULUM_END_DATE = LocalDate.EPOCH;
  private static final String CONNECTION_STATUS_1 = "connectectionStatus1";
  private static final String CONNECTION_STATUS_2 = "connectectionStatus2";
  private static final LocalDate DATA_ADDED_1 = LocalDate.MIN;
  private static final LocalDate DATA_ADDED_2 = LocalDate.MIN.plusYears(1);
  private static final String DESIGNATED_BODY_1 = "abc-1";
  private static final String DESIGNATED_BODY_2 = "abc-2";
  private static final String DOCTOR_STATUS_1 = "docStatus1";
  private static final String DOCTOR_STATUS_2 = "docStatus2";
  private static final String FIRST_NAME_1 = "Joe";
  private static final String FIRST_NAME_2 = "Jane";
  private static final String GMC_REF_NUMBER_1 = "gmc1";
  private static final String GMC_REF_NUMBER_2 = "gmc2";
  private static final String GMC_OUTCOME = "outcome";
  private static final String GRADE = "grade";
  private static final String LAST_NAME_1 = "Bloggs";
  private static final String LAST_NAME_2 = "Doe";
  private static final LocalDate LAST_UPDATED_DATE_1 = LocalDate.MAX;
  private static final LocalDate LAST_UPDATED_DATE_2 = LocalDate.MAX.minusYears(1);
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "type";
  private static final String PROGRAMME_NAME = "programme";
  private static final LocalDate SUBMISSION_DATE_1 = LocalDate.now();
  private static final LocalDate SUBMISSION_DATE_2 = LocalDate.now().plusYears(1);


  private DoctorRecommendationSummaryAggregationStrategy aggregationStrategy;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModules(new JavaTimeModule());
    aggregationStrategy = new DoctorRecommendationSummaryAggregationStrategy(objectMapper,
        new RecommendationSummaryMapperImpl(), new TraineeRecommendationMapperImpl());
  }

  @Test
  void shouldAggregateWhenMatchingGmcId() {
    final var traineeInfo1 = new TraineeInfoDto();
    traineeInfo1.setAdmin(ADMIN_1);
    traineeInfo1.setConnectionStatus(CONNECTION_STATUS_1);
    traineeInfo1.setDateAdded(DATA_ADDED_1);
    traineeInfo1.setDesignatedBody(DESIGNATED_BODY_1);
    traineeInfo1.setDoctorFirstName(FIRST_NAME_1);
    traineeInfo1.setDoctorLastName(LAST_NAME_1);
    traineeInfo1.setDoctorStatus(DOCTOR_STATUS_1);
    traineeInfo1.setGmcReferenceNumber(GMC_REF_NUMBER_1);
    traineeInfo1.setLastUpdatedDate(LAST_UPDATED_DATE_1);
    traineeInfo1.setSubmissionDate(SUBMISSION_DATE_1);

    final var traineeSummary = new TraineeSummaryDto();
    traineeSummary.setCountTotal(1);
    traineeSummary.setCountUnderNotice(2);
    traineeSummary.setTotalPages(3);
    traineeSummary.setTotalResults(4);
    traineeSummary.setTraineeInfo(List.of(traineeInfo1));

    final var camelContext = new DefaultCamelContext();
    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeSummary);
    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var traineeCore = new TraineeCoreDto();
    traineeCore.setCurriculumEndDate(CURRICULUM_END_DATE);
    traineeCore.setCurrentGrade(GRADE);
    traineeCore.setGmcOutcome(GMC_OUTCOME);
    traineeCore.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    traineeCore.setProgrammeName(PROGRAMME_NAME);

    final var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(GMC_REF_NUMBER_1, objectMapper.convertValue(traineeCore, Map.class)));
    final var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    final Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(RecommendationSummaryDto.class));

    final RecommendationSummaryDto aggregatedBody = aggregatedMessage
        .getBody(RecommendationSummaryDto.class);
    assertThat("Unexpected count total.", aggregatedBody.getCountTotal(), is(1L));
    assertThat("Unexpected under notice count.", aggregatedBody.getCountUnderNotice(), is(2L));
    assertThat("Unexpected total pages.", aggregatedBody.getTotalPages(), is(3L));
    assertThat("Unexpected total results.", aggregatedBody.getTotalResults(), is(4L));

    final List<RecommendationInfoDto> recommendationInfos = aggregatedBody.getRecommendationInfo();
    assertThat("Unexpected number of recommendation infos.", recommendationInfos.size(), is(1));

    final RecommendationInfoDto recommendationInfo = recommendationInfos.get(0);
    assertThat("Unexpected admin.", recommendationInfo.getAdmin(), is(ADMIN_1));
    assertThat("Unexpected curriculumEndDate date.",
        recommendationInfo.getCurriculumEndDate(), is(CURRICULUM_END_DATE));
    assertThat("Unexpected current grade.", recommendationInfo.getCurrentGrade(), is(GRADE));
    assertThat("Unexpected designated body.", recommendationInfo.getDesignatedBody(),
        is(DESIGNATED_BODY_1));
    assertThat("Unexpected first name.", recommendationInfo.getDoctorFirstName(), is(FIRST_NAME_1));
    assertThat("Unexpected last name.", recommendationInfo.getDoctorLastName(), is(LAST_NAME_1));
    assertThat("Unexpected doctor status.", recommendationInfo.getDoctorStatus(),
        is(DOCTOR_STATUS_1));
    assertThat("Unexpected GMC outcome.", recommendationInfo.getGmcOutcome(), is(GMC_OUTCOME));
    assertThat("Unexpected GMC reference number.", recommendationInfo.getGmcReferenceNumber(),
        is(GMC_REF_NUMBER_1));
    assertThat("Unexpected last updated date.", recommendationInfo.getLastUpdatedDate(),
        is(LAST_UPDATED_DATE_1));
    assertThat("Unexpected programme membership type.",
        recommendationInfo.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected programme name.", recommendationInfo.getProgrammeName(),
        is(PROGRAMME_NAME));
    assertThat("Unexpected submission date.", recommendationInfo.getSubmissionDate(),
        is(SUBMISSION_DATE_1));
  }

  @Test
  void shouldNotAggregateWhenNonMatchingGmcId() {
    final var traineeInfo1 = new TraineeInfoDto();
    traineeInfo1.setAdmin(ADMIN_1);
    traineeInfo1.setConnectionStatus(CONNECTION_STATUS_1);
    traineeInfo1.setDateAdded(DATA_ADDED_1);
    traineeInfo1.setDesignatedBody(DESIGNATED_BODY_1);
    traineeInfo1.setDoctorFirstName(FIRST_NAME_1);
    traineeInfo1.setDoctorLastName(LAST_NAME_1);
    traineeInfo1.setDoctorStatus(DOCTOR_STATUS_1);
    traineeInfo1.setGmcReferenceNumber(GMC_REF_NUMBER_1);
    traineeInfo1.setLastUpdatedDate(LAST_UPDATED_DATE_1);
    traineeInfo1.setSubmissionDate(SUBMISSION_DATE_1);

    final var traineeSummary = new TraineeSummaryDto();
    traineeSummary.setCountTotal(1);
    traineeSummary.setCountUnderNotice(2);
    traineeSummary.setTotalPages(3);
    traineeSummary.setTotalResults(4);
    traineeSummary.setTraineeInfo(List.of(traineeInfo1));

    final var camelContext = new DefaultCamelContext();
    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeSummary);
    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var traineeCore = new TraineeCoreDto();
    traineeCore.setCurriculumEndDate(CURRICULUM_END_DATE);
    traineeCore.setCurrentGrade(GRADE);
    traineeCore.setGmcOutcome(GMC_OUTCOME);
    traineeCore.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    traineeCore.setProgrammeName(PROGRAMME_NAME);

    final var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(GMC_REF_NUMBER_2, objectMapper.convertValue(traineeCore, Map.class)));
    final var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    final Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(RecommendationSummaryDto.class));

    final RecommendationSummaryDto aggregatedBody = aggregatedMessage
        .getBody(RecommendationSummaryDto.class);
    assertThat("Unexpected count total.", aggregatedBody.getCountTotal(), is(1L));
    assertThat("Unexpected under notice count.", aggregatedBody.getCountUnderNotice(), is(2L));
    assertThat("Unexpected total pages.", aggregatedBody.getTotalPages(), is(3L));
    assertThat("Unexpected total results.", aggregatedBody.getTotalResults(), is(4L));

    final List<RecommendationInfoDto> recommendationInfos = aggregatedBody.getRecommendationInfo();
    assertThat("Unexpected number of recommendation infos.", recommendationInfos.size(), is(1));

    final RecommendationInfoDto recommendationInfo = recommendationInfos.get(0);
    assertThat("Unexpected admin.", recommendationInfo.getAdmin(), is(ADMIN_1));
    assertThat("Unexpected curriculumEndDate date.",
        recommendationInfo.getCurriculumEndDate(), nullValue());
    assertThat("Unexpected current grade.", recommendationInfo.getCurrentGrade(), nullValue());
    assertThat("Unexpected designated body.", recommendationInfo.getDesignatedBody(),
        is(DESIGNATED_BODY_1));
    assertThat("Unexpected first name.", recommendationInfo.getDoctorFirstName(), is(FIRST_NAME_1));
    assertThat("Unexpected last name.", recommendationInfo.getDoctorLastName(), is(LAST_NAME_1));
    assertThat("Unexpected doctor status.", recommendationInfo.getDoctorStatus(),
        is(DOCTOR_STATUS_1));
    assertThat("Unexpected GMC outcome.", recommendationInfo.getGmcOutcome(), nullValue());
    assertThat("Unexpected GMC reference number.", recommendationInfo.getGmcReferenceNumber(),
        is(GMC_REF_NUMBER_1));
    assertThat("Unexpected last updated date.", recommendationInfo.getLastUpdatedDate(),
        is(LAST_UPDATED_DATE_1));
    assertThat("Unexpected programme membership type.",
        recommendationInfo.getProgrammeMembershipType(), nullValue());
    assertThat("Unexpected programme name.", recommendationInfo.getProgrammeName(), nullValue());
    assertThat("Unexpected submission date.", recommendationInfo.getSubmissionDate(),
        is(SUBMISSION_DATE_1));
  }

  @Test
  void shouldAggregateWhenMixedMatchingAndNonMatchingGmcId() {
    final var traineeInfo1 = new TraineeInfoDto();
    traineeInfo1.setAdmin(ADMIN_1);
    traineeInfo1.setConnectionStatus(CONNECTION_STATUS_1);
    traineeInfo1.setDateAdded(DATA_ADDED_1);
    traineeInfo1.setDesignatedBody(DESIGNATED_BODY_1);
    traineeInfo1.setDoctorFirstName(FIRST_NAME_1);
    traineeInfo1.setDoctorLastName(LAST_NAME_1);
    traineeInfo1.setDoctorStatus(DOCTOR_STATUS_1);
    traineeInfo1.setGmcReferenceNumber(GMC_REF_NUMBER_1);
    traineeInfo1.setLastUpdatedDate(LAST_UPDATED_DATE_1);
    traineeInfo1.setSubmissionDate(SUBMISSION_DATE_1);

    final var traineeInfo2 = new TraineeInfoDto();
    traineeInfo2.setAdmin(ADMIN_2);
    traineeInfo2.setConnectionStatus(CONNECTION_STATUS_2);
    traineeInfo2.setDateAdded(DATA_ADDED_2);
    traineeInfo2.setDesignatedBody(DESIGNATED_BODY_2);
    traineeInfo2.setDoctorFirstName(FIRST_NAME_2);
    traineeInfo2.setDoctorLastName(LAST_NAME_2);
    traineeInfo2.setDoctorStatus(DOCTOR_STATUS_2);
    traineeInfo2.setGmcReferenceNumber(GMC_REF_NUMBER_2);
    traineeInfo2.setLastUpdatedDate(LAST_UPDATED_DATE_2);
    traineeInfo2.setSubmissionDate(SUBMISSION_DATE_2);

    final var traineeSummary = new TraineeSummaryDto();
    traineeSummary.setCountTotal(1);
    traineeSummary.setCountUnderNotice(2);
    traineeSummary.setTotalPages(3);
    traineeSummary.setTotalResults(4);
    traineeSummary.setTraineeInfo(List.of(traineeInfo1, traineeInfo2));

    final var camelContext = new DefaultCamelContext();
    final var oldMessage = new DefaultMessage(camelContext);
    oldMessage.setBody(traineeSummary);
    final var oldExchange = new DefaultExchange(camelContext);
    oldExchange.setIn(oldMessage);

    final var traineeCore = new TraineeCoreDto();
    traineeCore.setCurriculumEndDate(CURRICULUM_END_DATE);
    traineeCore.setCurrentGrade(GRADE);
    traineeCore.setGmcOutcome(GMC_OUTCOME);
    traineeCore.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    traineeCore.setProgrammeName(PROGRAMME_NAME);

    final var newMessage = new DefaultMessage(camelContext);
    newMessage.setBody(Map.of(GMC_REF_NUMBER_1, objectMapper.convertValue(traineeCore, Map.class)));
    final var newExchange = new DefaultExchange(camelContext);
    newExchange.setIn(newMessage);

    final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, newExchange);
    final Message aggregatedMessage = aggregatedExchange.getMessage();
    assertThat("Unexpected message body type.", aggregatedMessage.getBody(),
        instanceOf(RecommendationSummaryDto.class));

    final RecommendationSummaryDto aggregatedBody = aggregatedMessage
        .getBody(RecommendationSummaryDto.class);

    final List<RecommendationInfoDto> recommendationInfos = aggregatedBody.getRecommendationInfo();
    assertThat("Unexpected number of recommendation infos.", recommendationInfos.size(), is(2));

    RecommendationInfoDto recommendationInfo = recommendationInfos.get(0);
    assertThat("Unexpected GMC reference number.", recommendationInfo.getGmcReferenceNumber(),
        is(GMC_REF_NUMBER_1));
    assertThat("Unexpected curriculumEndDate date.",
        recommendationInfo.getCurriculumEndDate(), is(CURRICULUM_END_DATE));
    assertThat("Unexpected current grade.", recommendationInfo.getCurrentGrade(), is(GRADE));
    assertThat("Unexpected GMC outcome.", recommendationInfo.getGmcOutcome(), is(GMC_OUTCOME));
    assertThat("Unexpected programme membership type.",
        recommendationInfo.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected programme name.", recommendationInfo.getProgrammeName(),
        is(PROGRAMME_NAME));

    recommendationInfo = recommendationInfos.get(1);
    assertThat("Unexpected GMC reference number.", recommendationInfo.getGmcReferenceNumber(),
        is(GMC_REF_NUMBER_2));
    assertThat("Unexpected curriculumEndDate date.",
        recommendationInfo.getCurriculumEndDate(), nullValue());
    assertThat("Unexpected current grade.", recommendationInfo.getCurrentGrade(), nullValue());
    assertThat("Unexpected GMC outcome.", recommendationInfo.getGmcOutcome(), nullValue());
    assertThat("Unexpected programme membership type.",
        recommendationInfo.getProgrammeMembershipType(), nullValue());
    assertThat("Unexpected programme name.", recommendationInfo.getProgrammeName(), nullValue());
  }
}
