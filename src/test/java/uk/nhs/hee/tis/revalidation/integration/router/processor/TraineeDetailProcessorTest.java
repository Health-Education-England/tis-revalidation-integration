package uk.nhs.hee.tis.revalidation.integration.router.processor;

import static java.time.LocalDate.now;
import static org.elasticsearch.common.inject.matcher.Matchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.HashMap;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@ExtendWith(MockitoExtension.class)
class TraineeDetailProcessorTest {

  private final Faker faker = new Faker();
  private final String traineeAggregationHeader = "programme";
  private final String gmcAggregationHeader = "doctor";
  private final String notesAggregationHeader = "notes";
  private final  ObjectMapper jsonMapper = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .build();

  @InjectMocks
  TraineeDetailProcessor traineeDetailProcessor;
  @Spy
  ObjectMapper mapper;
  @Captor
  ArgumentCaptor<TraineeDetailsDto> traineeDetailsDtoCaptor;
  Exchange exchange;
  Map<String, Object> exchangeMapAll;
  Map<String, Object> exchangeMapGmc;
  Map<String, Object> exchangeMapTrainee;

  private TraineeSummaryDto gmcDoctorDetails;
  private TraineeDetailsDto traineeDetails;
  private TraineeNotesDto notes;
  private String gmcDoctorDetailsMessageString;
  private String traineeDetailsMessageString;
  private String notesString;
  private String gmcRef1;
  private String firstName1, firstName2;
  private String lastName1, lastName2;
  private LocalDate submissionDate1;
  private LocalDate curriculumEndDate1;
  private LocalDate dateAdded1;
  private RecommendationStatus doctorStatus1;
  private String admin;
  private String connectionStatus1;
  private String programmeName;
  private String programmeMembershipType;
  private String currentGrade;
  private Integer tisPersonId;


  @BeforeEach
  void setup() {
    CamelContext ctx = new DefaultCamelContext();
    exchange = new DefaultExchange(ctx);
    populateTestData();
  }

  @Test
  void shouldUseTraineeDetailsIfNoGmcDoctorData() throws Exception {
    Message message = new DefaultMessage(exchange);
    message.setBody(jsonMapper.writeValueAsString(exchangeMapTrainee));
    exchange.setIn(message);

    traineeDetailProcessor.process(exchange);

    verify(exchange).getIn().setBody(traineeDetailsDtoCaptor.capture());
  }

  @Test
  void shouldUseGmcDoctorDetailsIfNoTraineeData() {

  }

  @Test
  void shouldUpdateNotes() {

  }

  @Test
  void shouldMergeDoctorDetailsAndTraineeDetailsAndDefaultToGmcDoctorDetails() {

  }

  private void populateTestData() {
    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    firstName2 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    lastName2 = faker.name().lastName();
    submissionDate1 = now();
    curriculumEndDate1 = now();
    dateAdded1 = now().minusDays(5);
    doctorStatus1 = RecommendationStatus.SUBMITTED_TO_GMC;
    admin = faker.internet().emailAddress();
    connectionStatus1 = faker.lorem().characters(3);
    programmeName = faker.lorem().characters(15);
    programmeMembershipType = faker.lorem().characters(10);
    currentGrade = faker.lorem().characters(10);
    tisPersonId = Integer.valueOf(faker.number().digits(7));

    final var doctors = List.of(TraineeInfoDto.builder()
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1)
        .doctorLastName(lastName1)
        .submissionDate(submissionDate1)
        .dateAdded(dateAdded1)
        .doctorStatus(doctorStatus1.name())
        .connectionStatus(connectionStatus1)
        .admin(admin)
        .build()
    );

    gmcDoctorDetails = TraineeSummaryDto.builder()
        .traineeInfo(doctors)
        .countTotal(doctors.size())
        .countUnderNotice(1L)
        .build();

    traineeDetails = TraineeDetailsDto.builder()
        .gmcNumber(gmcRef1)
        .forenames(firstName2)
        .surname(lastName2)
        .curriculumEndDate(curriculumEndDate1)
        .programmeName(programmeName)
        .programmeMembershipType(programmeMembershipType)
        .currentGrade(currentGrade)
        .tisPersonId(tisPersonId)
        .build();

    final var testNotes = List.of(TraineeNotesInfoDto.builder()
        .id(faker.idNumber().valid())
        .gmcId(gmcRef1)
        .text(faker.lorem().characters(100))
        .createdDate(LocalDate.now())
        .updatedDate(LocalDate.now())
        .build());

    notes = TraineeNotesDto.builder()
        .gmcId(gmcRef1)
        .notes(testNotes)
        .build();

    exchangeMapTrainee = Map.of(traineeAggregationHeader, traineeDetails);


  }

}
