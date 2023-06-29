/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.nhs.hee.tis.revalidation.integration.router.processor.TraineeDetailProcessor.GMC_AGGREGATION_HEADER;
import static uk.nhs.hee.tis.revalidation.integration.router.processor.TraineeDetailProcessor.NOTES_AGGREGATION_HEADER;
import static uk.nhs.hee.tis.revalidation.integration.router.processor.TraineeDetailProcessor.TRAINEE_AGGREGATION_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@ExtendWith(MockitoExtension.class)
class TraineeDetailProcessorTest {

  private final Faker faker = new Faker();
  TraineeDetailProcessor traineeDetailProcessor;
  ObjectMapper jsonMapper;
  Exchange exchange;
  private TraineeSummaryDto gmcDoctorDetails;
  private TraineeSummaryDto noGmcDoctorDetails;
  private TraineeDetailsDto tcsTraineeDetails;
  private TraineeNotesDto notes;
  private TraineeNotesDto noNotes;
  private String gmcNumber;
  private String firstName1;
  private String firstName2;
  private String lastName1;
  private String lastName2;
  private LocalDate submissionDate;
  private LocalDate curriculumEndDate;
  private LocalDate dateAdded;
  private RecommendationStatus doctorStatus;
  private String admin;
  private String connectionStatus;
  private String programmeName;
  private String programmeMembershipType;
  private String currentGrade;
  private Integer tisPersonId;

  @BeforeEach
  void setup() {
    jsonMapper = new ObjectMapper();
    jsonMapper.registerModule(new JavaTimeModule());
    traineeDetailProcessor = new TraineeDetailProcessor(jsonMapper);

    CamelContext ctx = new DefaultCamelContext();
    exchange = new DefaultExchange(ctx);
    populateTestData();
  }

  @Test
  void shouldUseTraineeDetailsIfNoGmcDoctorData() throws Exception {
    Message message = new DefaultMessage(exchange);

    Map<String, Object> bodyMap = Map.of(
        TRAINEE_AGGREGATION_HEADER, tcsTraineeDetails,
        GMC_AGGREGATION_HEADER, noGmcDoctorDetails,
        NOTES_AGGREGATION_HEADER, notes);
    message.setBody(jsonMapper.writeValueAsString(bodyMap));
    exchange.setIn(message);

    traineeDetailProcessor.process(exchange);

    TraineeDetailsDto dto = exchange.getIn().getBody(TraineeDetailsDto.class);
    assertThat(dto.getGmcNumber(), is(gmcNumber));
    assertThat(dto.getForenames(), is(firstName1));
    assertThat(dto.getSurname(), is(lastName1));
    assertThat(dto.getCurrentGrade(), is(currentGrade));
    assertThat(dto.getProgrammeName(), is(programmeName));
    assertThat(dto.getCurriculumEndDate(), is(curriculumEndDate));
    assertThat(dto.getProgrammeMembershipType(), is(programmeMembershipType));
    assertThat(dto.getTisPersonId(), is(tisPersonId));
    assertThat(dto.getNotes().size(), equalTo(1));
    assertThat(dto.getNotes(), is(notes.getNotes()));
  }

  @Test
  void shouldUseGmcDoctorDetailsToOverwriteTraineeNames() throws Exception {
    Message message = new DefaultMessage(exchange);

    Map<String, Object> bodyMap = Map.of(
        TRAINEE_AGGREGATION_HEADER, tcsTraineeDetails,
        GMC_AGGREGATION_HEADER, gmcDoctorDetails,
        NOTES_AGGREGATION_HEADER, notes);
    message.setBody(jsonMapper.writeValueAsString(bodyMap));
    exchange.setIn(message);

    traineeDetailProcessor.process(exchange);

    TraineeDetailsDto dto = exchange.getIn().getBody(TraineeDetailsDto.class);
    assertThat(dto.getGmcNumber(), is(gmcNumber));
    assertThat(dto.getForenames(), is(firstName2));
    assertThat(dto.getSurname(), is(lastName2));
    assertThat(dto.getCurrentGrade(), is(currentGrade));
    assertThat(dto.getProgrammeName(), is(programmeName));
    assertThat(dto.getCurriculumEndDate(), is(curriculumEndDate));
    assertThat(dto.getProgrammeMembershipType(), is(programmeMembershipType));
    assertThat(dto.getTisPersonId(), is(tisPersonId));
    assertThat(dto.getNotes().size(), equalTo(1));
    assertThat(dto.getNotes(), is(notes.getNotes()));
  }

  @Test
  void shouldUseGmcDoctorDetailsIfNoTraineeData() throws Exception {
    Message message = new DefaultMessage(exchange);

    Map<String, Object> bodyMap = Map.of(
        TRAINEE_AGGREGATION_HEADER, TraineeDetailsDto.builder().build(),
        GMC_AGGREGATION_HEADER, gmcDoctorDetails,
        NOTES_AGGREGATION_HEADER, notes);
    message.setBody(jsonMapper.writeValueAsString(bodyMap));
    exchange.setIn(message);

    traineeDetailProcessor.process(exchange);

    TraineeDetailsDto dto = exchange.getIn().getBody(TraineeDetailsDto.class);
    assertThat(dto.getGmcNumber(), is(gmcNumber));
    assertThat(dto.getForenames(), is(firstName2));
    assertThat(dto.getSurname(), is(lastName2));
    assertThat(dto.getCurrentGrade(), nullValue());
    assertThat(dto.getProgrammeName(), nullValue());
    assertThat(dto.getCurriculumEndDate(), nullValue());
    assertThat(dto.getProgrammeMembershipType(), nullValue());
    assertThat(dto.getTisPersonId(), nullValue());
    assertThat(dto.getNotes().size(), equalTo(1));
    assertThat(dto.getNotes(), is(notes.getNotes()));
  }

  @Test
  void shouldSet404HeaderWhenNoTraineeDetailsFound() throws Exception {
    Message message = new DefaultMessage(exchange);

    Map<String, Object> bodyMap = Map.of(
        TRAINEE_AGGREGATION_HEADER, TraineeDetailsDto.builder().build(),
        GMC_AGGREGATION_HEADER, noGmcDoctorDetails,
        NOTES_AGGREGATION_HEADER, noNotes);
    message.setBody(jsonMapper.writeValueAsString(bodyMap));
    exchange.setIn(message);

    traineeDetailProcessor.process(exchange);
    assertThat(exchange.getIn().getBody(), nullValue());
    assertThat(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE),
        equalTo(HttpStatus.NOT_FOUND.value()));
  }

  private void populateTestData() {
    gmcNumber = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    firstName2 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    lastName2 = faker.name().lastName();
    submissionDate = now();
    curriculumEndDate = now();
    dateAdded = now().minusDays(5);
    doctorStatus = RecommendationStatus.SUBMITTED_TO_GMC;
    admin = faker.internet().emailAddress();
    connectionStatus = faker.lorem().characters(3);
    programmeName = faker.lorem().characters(15);
    programmeMembershipType = faker.lorem().characters(10);
    currentGrade = faker.lorem().characters(10);
    tisPersonId = Integer.valueOf(faker.number().digits(7));

    final var doctors = List.of(TraineeInfoDto.builder()
        .gmcReferenceNumber(gmcNumber)
        .doctorFirstName(firstName2)
        .doctorLastName(lastName2)
        .submissionDate(submissionDate)
        .dateAdded(dateAdded)
        .doctorStatus(doctorStatus.name())
        .connectionStatus(connectionStatus)
        .admin(admin)
        .build()
    );

    gmcDoctorDetails = TraineeSummaryDto.builder()
        .traineeInfo(doctors)
        .countTotal(doctors.size())
        .countUnderNotice(1L)
        .build();

    noGmcDoctorDetails = TraineeSummaryDto.builder()
        .traineeInfo(new ArrayList<>())
        .countTotal(0L)
        .countUnderNotice(0L)
        .build();

    tcsTraineeDetails = TraineeDetailsDto.builder()
        .gmcNumber(gmcNumber)
        .forenames(firstName1)
        .surname(lastName1)
        .curriculumEndDate(curriculumEndDate)
        .programmeName(programmeName)
        .programmeMembershipType(programmeMembershipType)
        .currentGrade(currentGrade)
        .tisPersonId(tisPersonId)
        .build();

    final var testNotes = List.of(TraineeNotesInfoDto.builder()
        .id(faker.idNumber().valid())
        .gmcId(gmcNumber)
        .text(faker.lorem().characters(100))
        .createdDate(LocalDate.now())
        .updatedDate(LocalDate.now())
        .build());

    notes = TraineeNotesDto.builder()
        .gmcId(gmcNumber)
        .notes(testNotes)
        .build();

    noNotes = TraineeNotesDto.builder()
        .gmcId(gmcNumber)
        .notes(new ArrayList<>())
        .build();
  }
}
