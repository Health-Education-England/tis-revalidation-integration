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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcTraineeUpdateServiceTest {

  private final MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();
  private final Long tcsPersonId = 1L;
  private final String gmcRefereneNumber = "1234567";
  private final String doctorFirstName = "doctorFirstName";
  private final String doctorLastName = "doctorLastName";
  private final LocalDate submissionDate = LocalDate.now();
  private final String programmeName = "programmeName";
  private final String programmeMembershipType = "programmeMembershipType";
  private final String designatedBody = "designatedBody";
  private final String tcsDesignatedBody = "tcsDesignatedBody";
  private final String programmeOwner = "programmeOwner";
  private final String connectionStatus = "connectionStatus";
  private final LocalDate programmeMembershipStartDate = LocalDate.now();
  private final LocalDate programmeMembershipEndDate = LocalDate.now();
  private final LocalDate curriculumEndDate = LocalDate.now();
  private final String dataSource = "dataSource";
  @InjectMocks
  private CdcTraineeUpdateService cdcTraineeUpdateService;
  @Mock
  private MasterDoctorElasticSearchRepository repository;
  @Mock
  private CdcMessagePublisher publisher;
  @Spy
  private MasterDoctorViewMapper mapper = new MasterDoctorViewMapperImpl();
  @Captor
  private ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  private ConnectionInfoDto traineeUpdate;

  @BeforeEach
  void setupTestData() {
    traineeUpdate =
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
  }

  @Test
  void shouldUpsertNewFields() {
    when(repository.findByGmcReferenceNumber(gmcRefereneNumber))
        .thenReturn(List.of(masterDoctorView));
    traineeUpdate.setGmcReferenceNumber(gmcRefereneNumber);

    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

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
    final var masterDoctorViewNullGmc = CdcTestDataGenerator.getTestMasterDoctorView();
    masterDoctorViewNullGmc.setGmcReferenceNumber(null);
    when(repository.findByTcsPersonId(tcsPersonId)).thenReturn(List.of(masterDoctorViewNullGmc));
    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

    verify(repository).save(masterDoctorViewCaptor.capture());

    //new fields
    final var savedEntity = masterDoctorViewCaptor.getValue();
    assertThat(savedEntity.getDoctorFirstName(), is(doctorFirstName));
    assertThat(savedEntity.getDoctorLastName(), is(doctorLastName));
    assertThat(savedEntity.getTcsPersonId(), is(tcsPersonId));
  }

  @ParameterizedTest(name = "Should Find Existing by TIS ID if GMC Number is [{0}]")
  @NullAndEmptySource
  @ValueSource(strings = {"Unknown", "UNKNOWN", "unknown", " "})
  void shouldFindExistingByTisIdIfUnknownGmc(String unknown) {
    traineeUpdate.setGmcReferenceNumber(unknown);
    when(repository.findByTcsPersonId(tcsPersonId)).thenReturn(List.of(masterDoctorView));

    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

    verify(repository, never()).findByGmcReferenceNumber(any());
  }

  @Test
  void shouldInsertTraineeInfoIfNoMatch() {
    final var masterDoctorViewNullGmc = CdcTestDataGenerator.getTestMasterDoctorView();
    masterDoctorViewNullGmc.setGmcReferenceNumber(null);
    when(repository.findByTcsPersonId(any())).thenReturn(Collections.emptyList());

    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

    verify(repository).save(masterDoctorViewCaptor.capture());

    //new fields
    final var savedMasterDoctorView = masterDoctorViewCaptor.getValue();
    assertThat(savedMasterDoctorView.getDoctorFirstName(), is(doctorFirstName));
    assertThat(savedMasterDoctorView.getDoctorLastName(), is(doctorLastName));
    assertThat(savedMasterDoctorView.getTcsPersonId(), is(tcsPersonId));
  }

  @Test
  void shouldPublishUpdateForGmcNumber() {
    MasterDoctorView view2 = CdcTestDataGenerator.getTestMasterDoctorView();
    traineeUpdate.setGmcReferenceNumber(gmcRefereneNumber);
    when(repository.findByGmcReferenceNumber(gmcRefereneNumber))
        .thenReturn(List.of(masterDoctorView, view2));
    MasterDoctorView updatedView = new MasterDoctorView();
    when(repository.save(any())).thenReturn(updatedView);

    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

    verify(publisher).publishCdcUpdate(updatedView);
  }

  @Test
  void shouldPublishUpdateForTcsId() {
    MasterDoctorView view2 = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByTcsPersonId(tcsPersonId)).thenReturn(List.of(masterDoctorView, view2));
    MasterDoctorView updatedView = new MasterDoctorView();
    when(repository.save(any())).thenReturn(updatedView);

    cdcTraineeUpdateService.upsertEntity(traineeUpdate);

    verify(publisher).publishCdcUpdate(updatedView);
  }

}
