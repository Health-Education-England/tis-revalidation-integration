/*
 * The MIT License (MIT)
 *
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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcTraineeUpdateService;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcTraineeUpdateServiceTest {

  @InjectMocks
  CdcTraineeUpdateService cdcTraineeUpdateService;

  @Mock
  MasterDoctorElasticSearchRepository repository;

  @Mock
  CdcMessagePublisher publisher;

  @Spy
  MasterDoctorViewMapper mapper = new MasterDoctorViewMapperImpl();

  @Captor
  ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  private MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();

  private Long tcsPersonId = 1L;
  private String gmcRefereneNumber = "1234567";
  private String doctorFirstName = "doctorFirstName";
  private String doctorLastName = "doctorLastName";
  private LocalDate submissionDate = LocalDate.now();
  private String programmeName = "programmeName";
  private String programmeMembershipType = "programmeMembershipType";
  private String designatedBody = "designatedBody";
  private String tcsDesignatedBody = "tcsDesignatedBody";
  private String programmeOwner = "programmeOwner";
  private String connectionStatus = "connectionStatus";
  private LocalDate programmeMembershipStartDate = LocalDate.now();
  private LocalDate programmeMembershipEndDate = LocalDate.now();
  private LocalDate curriculumEndDate = LocalDate.now();
  private String dataSource = "dataSource";

  @Test
  void shouldUpsertNewFields() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var traineeUpdates =
        ConnectionInfoDto.builder()
            .tcsPersonId(tcsPersonId)
            .gmcReferenceNumber(gmcRefereneNumber)
            .doctorFirstName(doctorFirstName)
            .doctorLastName(doctorLastName)
            .submissionDate(submissionDate)
            .programmeName(programmeName)
            .programmeMembershipType(programmeMembershipType)
            .designatedBody(designatedBody)
            .tcsDesignatedBody(tcsDesignatedBody)
            .programmeOwner(programmeOwner)
            .connectionStatus(connectionStatus)
            .programmeMembershipStartDate(programmeMembershipStartDate)
            .programmeMembershipEndDate(programmeMembershipEndDate)
            .curriculumEndDate(curriculumEndDate)
            .dataSource(dataSource)
            .build();
    cdcTraineeUpdateService.upsertEntity(traineeUpdates);

    verify(repository).save(masterDoctorViewCaptor.capture());

    //new fields
    final var savedEntity = masterDoctorViewCaptor.getValue();
    assertThat(savedEntity.getDoctorFirstName(), is(doctorFirstName));
    assertThat(savedEntity.getDoctorLastName(), is(doctorLastName));
    assertThat(savedEntity.getTcsPersonId(), is(tcsPersonId));
    //existing fields
    assertThat(savedEntity.getGmcReferenceNumber(), is(masterDoctorView.getGmcReferenceNumber()));
    assertThat(savedEntity.getTisStatus(), is(masterDoctorView.getTisStatus()));
    assertThat(savedEntity.getAdmin(), is(masterDoctorView.getAdmin()));

  }

  @Test
  void shouldUpsertTraineeInfoIfGmcNumberNull() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());
    final var masterDoctorViewNullGmc = CdcTestDataGenerator.getTestMasterDoctorView();
    masterDoctorViewNullGmc.setGmcReferenceNumber(null);

    when(repository.findByTcsPersonId(any())).thenReturn(List.of(masterDoctorViewNullGmc));

    var traineeUpdates =
            ConnectionInfoDto.builder()
            .tcsPersonId(tcsPersonId)
            .gmcReferenceNumber(null)
            .doctorFirstName(doctorFirstName)
            .doctorLastName(doctorLastName)
            .submissionDate(submissionDate)
            .programmeName(programmeName)
            .programmeMembershipType(programmeMembershipType)
            .designatedBody(designatedBody)
            .tcsDesignatedBody(tcsDesignatedBody)
            .programmeOwner(programmeOwner)
            .connectionStatus(connectionStatus)
            .programmeMembershipStartDate(programmeMembershipStartDate)
            .programmeMembershipEndDate(programmeMembershipEndDate)
            .curriculumEndDate(curriculumEndDate)
            .dataSource(dataSource)
            .build();
    cdcTraineeUpdateService.upsertEntity(traineeUpdates);

    verify(repository).save(masterDoctorViewCaptor.capture());

    //new fields
    final var savedEntity = masterDoctorViewCaptor.getValue();
    assertThat(savedEntity.getDoctorFirstName(), is(doctorFirstName));
    assertThat(savedEntity.getDoctorLastName(), is(doctorLastName));
    assertThat(savedEntity.getTcsPersonId(), is(tcsPersonId));
  }

  @Test
  void shouldInsertTraineeInfoIfGmcNumberNull() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());
    final var masterDoctorViewNullGmc = CdcTestDataGenerator.getTestMasterDoctorView();
    masterDoctorViewNullGmc.setGmcReferenceNumber(null);

    when(repository.findByTcsPersonId(any())).thenReturn(Collections.emptyList());

    var traineeUpdates =
        ConnectionInfoDto.builder()
            .tcsPersonId(tcsPersonId)
            .gmcReferenceNumber(null)
            .doctorFirstName(doctorFirstName)
            .doctorLastName(doctorLastName)
            .submissionDate(submissionDate)
            .programmeName(programmeName)
            .programmeMembershipType(programmeMembershipType)
            .designatedBody(designatedBody)
            .tcsDesignatedBody(tcsDesignatedBody)
            .programmeOwner(programmeOwner)
            .connectionStatus(connectionStatus)
            .programmeMembershipStartDate(programmeMembershipStartDate)
            .programmeMembershipEndDate(programmeMembershipEndDate)
            .curriculumEndDate(curriculumEndDate)
            .dataSource(dataSource)
            .build();
    cdcTraineeUpdateService.upsertEntity(traineeUpdates);

    verify(repository).save(masterDoctorViewCaptor.capture());

    //new fields
    final var savedEntity = masterDoctorViewCaptor.getValue();
    assertThat(savedEntity.getDoctorFirstName(), is(doctorFirstName));
    assertThat(savedEntity.getDoctorLastName(), is(doctorLastName));
    assertThat(savedEntity.getTcsPersonId(), is(tcsPersonId));
  }

  @Test
  void shouldPublishUpdates() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(repository.save(any())).thenReturn(masterDoctorView);

    var traineeUpdates =
        ConnectionInfoDto.builder().build();
    cdcTraineeUpdateService.upsertEntity(traineeUpdates);

    verify(publisher).publishCdcUpdate(masterDoctorView);
  }

}
