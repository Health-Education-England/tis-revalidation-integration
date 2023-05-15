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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcDoctorService;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcDoctorServiceTest {

  @InjectMocks
  CdcDoctorService cdcDoctorService;

  @Mock
  MasterDoctorElasticSearchRepository repository;

  @Mock
  CdcMessagePublisher publisher;

  @Mock
  MasterDoctorViewMapper mapper;

  @Captor
  ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  private MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();

  @Test
  void shouldAddNewFieldsIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto()
        .getFullDocument();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(repository).save(mapper.doctorToMasterView(newDoctor));
  }

  @Test
  void shouldUpdateFieldsIfDoctorExistsOnAdd() {
    var existingDoctor = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(existingDoctor));

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto()
        .getFullDocument();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(mapper).updateMasterDoctorView(existingDoctor, mapper.doctorToMasterView(newDoctor));
    verify(repository).save(any());
  }

  @Test
  void shouldSetDesignatedBodyCodeToNull() {
    var existingDoctor = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(existingDoctor));

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctorNullDbcInsertCdcDocumentDto()
        .getFullDocument();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(mapper).updateMasterDoctorView(existingDoctor, mapper.doctorToMasterView(newDoctor));
    verify(repository).save(masterDoctorViewCaptor.capture());

    assertNull(masterDoctorViewCaptor.getValue().getDesignatedBody());
  }

  @Test
  void shouldPublishUpdates() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(repository.save(any())).thenReturn(masterDoctorView);

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto()
        .getFullDocument();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(publisher).publishCdcUpdate(masterDoctorView);
  }
}
