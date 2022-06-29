/*
 * The MIT License (MIT)
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.sync.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.sync.listener.TraineeDataMessageListener;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;


@ExtendWith(MockitoExtension.class)
class TraineeDataMessageListenerTest {

  @InjectMocks
  private TraineeDataMessageListener traineeDataMessageListener;

  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  @Mock
  private RabbitTemplate rabbitTemplate;

  private ConnectionInfoDto connectionInfo;
  private MasterDoctorView masterDoctorView;
  private Faker faker = new Faker();
  private Long tcsPersonId;
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  private LocalDate submissionDate;
  private String programmeName;
  private String programmeMembershipType;
  private String designatedBody;
  private String tcsDesignatedBody;
  private String programmeOwner;
  private String connectionStatus;
  private LocalDate programmeMembershipStartDate;
  private LocalDate programmeMembershipEndDate;
  private LocalDate curriculumEndDate;
  private Boolean syncEnd;
  private static final String GMC_SYNC_START = "gmcSyncStart";


  @BeforeEach
  void setUp() {
    tcsPersonId = 001L;
    gmcReferenceNumber = faker.number().digits(8);
    doctorFirstName = faker.name().firstName();
    doctorLastName = faker.name().lastName();
    submissionDate = LocalDate.now();
    programmeName = faker.lorem().characters(20);
    programmeMembershipType = faker.lorem().characters(20);
    designatedBody = faker.lorem().characters(20);
    tcsDesignatedBody = faker.lorem().characters(20);
    programmeOwner = faker.lorem().characters(20);
    connectionStatus = faker.lorem().characters(20);
    programmeMembershipStartDate = LocalDate.now();
    programmeMembershipEndDate = LocalDate.now();
    curriculumEndDate = LocalDate.now();

    connectionInfo = ConnectionInfoDto.builder()
        .tcsPersonId(001L)
        .gmcReferenceNumber(gmcReferenceNumber)
        .doctorFirstName(doctorFirstName)
        .doctorLastName(doctorLastName)
        .programmeName(programmeName)
        .programmeMembershipType(programmeMembershipType)
        .designatedBody(designatedBody)
        .tcsDesignatedBody(tcsDesignatedBody)
        .programmeOwner(programmeOwner)
        .programmeMembershipStartDate(programmeMembershipStartDate)
        .programmeMembershipEndDate(programmeMembershipEndDate)
        .curriculumEndDate(curriculumEndDate)
        .build();

    masterDoctorView = MasterDoctorView.builder()
        .tcsPersonId(001L)
        .gmcReferenceNumber(gmcReferenceNumber)
        .doctorFirstName(doctorFirstName)
        .doctorLastName(doctorLastName)
        .submissionDate(submissionDate)
        .programmeName(programmeName)
        .membershipType(programmeMembershipType)
        .designatedBody(designatedBody)
        .tcsDesignatedBody(tcsDesignatedBody)
        .programmeOwner(programmeOwner)
        .connectionStatus(connectionStatus)
        .membershipStartDate(programmeMembershipStartDate)
        .membershipEndDate(programmeMembershipEndDate)
        .curriculumEndDate(curriculumEndDate)
        .build();

    setField(traineeDataMessageListener, "exchange", "exchange");
    setField(traineeDataMessageListener, "routingKey", "routingKey");
  }

  @Test
  void shouldPopulateMasterIndexWhenNotSyncEnd() {
    connectionInfo.setSyncEnd(null);

    traineeDataMessageListener.receiveMessage(connectionInfo);

    ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor = ArgumentCaptor
        .forClass(MasterDoctorView.class);
    verify(doctorUpsertElasticSearchService).populateMasterIndex(masterDoctorViewCaptor.capture());
    MasterDoctorView masterDoctorView = masterDoctorViewCaptor.getValue();

    assertThat(masterDoctorView.getGmcReferenceNumber(), is(gmcReferenceNumber));
    assertThat(masterDoctorView.getDoctorFirstName(), is(doctorFirstName));
    assertThat(masterDoctorView.getDoctorLastName(), is(doctorLastName));
    assertThat(masterDoctorView.getProgrammeName(), is(programmeName));
    assertThat(masterDoctorView.getMembershipType(), is(programmeMembershipType));
    assertThat(masterDoctorView.getDesignatedBody(), is(designatedBody));
    assertThat(masterDoctorView.getTcsDesignatedBody(), is(tcsDesignatedBody));
    assertThat(masterDoctorView.getProgrammeOwner(), is(programmeOwner));
    assertThat(masterDoctorView.getMembershipStartDate(), is(programmeMembershipStartDate));
    assertThat(masterDoctorView.getMembershipEndDate(), is(programmeMembershipEndDate));
    assertThat(masterDoctorView.getCurriculumEndDate(), is(curriculumEndDate));
  }

  @Test
  void shouldSendSyncStartMessageWhenSyncEnd() {
    connectionInfo.setSyncEnd(true);

    traineeDataMessageListener.receiveMessage(connectionInfo);
    verify(rabbitTemplate).convertAndSend("exchange", "routingKey", GMC_SYNC_START);
  }
}
