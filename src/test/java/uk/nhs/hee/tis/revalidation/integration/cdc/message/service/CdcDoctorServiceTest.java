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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.ADMIN;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DESIGNATED_BODY_CODE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_FIRST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_LAST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_STATUS;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.EXISTS_IN_GMC;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.LAST_UPDATED_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.SUBMISSION_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.UNDER_NOTICE;

import java.util.Collections;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.util.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcDoctorService;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcDoctorFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class CdcDoctorServiceTest {

  @InjectMocks
  CdcDoctorService cdcDoctorService;

  @Mock
  MasterDoctorElasticSearchRepository repository;

  @Mock
  CdcDoctorFieldUpdateHelper fieldUpdateHelper;

  @Mock
  MasterDoctorViewMapper mapper;

  @Test
  void shouldAddNewFieldsIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    DoctorsForDB newDoctor =
        CdcTestDataGenerator.getDoctorInsertChangeStreamDocument().getFullDocument();
    cdcDoctorService.addNewEntity(newDoctor);

    verify(repository).save(mapper.doctorToMasterView(newDoctor));
  }

  @Test
  void shouldUpdateFieldsIfDoctorExistsOnAdd() {
    var existingDoctor = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(existingDoctor)
    );

    DoctorsForDB newDoctor =
        CdcTestDataGenerator.getDoctorInsertChangeStreamDocument().getFullDocument();
    cdcDoctorService.addNewEntity(newDoctor);

    verify(mapper).updateMasterDoctorView(existingDoctor, mapper.doctorToMasterView(newDoctor));
    verify(repository).save(any());
  }

  @Test
  void shouldUpdateSubsetOfFields() {
    var masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var changes =
        CdcTestDataGenerator.getDoctorUpdateChangeStreamDocument();
    cdcDoctorService.updateSubsetOfFields(changes);

    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, DOCTOR_FIRST_NAME, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, DOCTOR_LAST_NAME, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, SUBMISSION_DATE, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, UNDER_NOTICE, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, DOCTOR_STATUS, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, LAST_UPDATED_DATE, changes.getUpdateDescription().getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, DESIGNATED_BODY_CODE, changes
                .getUpdateDescription()
                .getUpdatedFields()
        );
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, ADMIN, changes.getUpdateDescription().getUpdatedFields());
    verify(fieldUpdateHelper)
        .updateField(
            masterDoctorView, EXISTS_IN_GMC, changes.getUpdateDescription().getUpdatedFields());
  }

  @Test
  void shouldNotUpdateSubsetOfFieldsIfDoctorDoesNotExist() {
    var masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    var changes =
        CdcTestDataGenerator.getDoctorUpdateChangeStreamDocument();
    cdcDoctorService.updateSubsetOfFields(changes);

    verify(repository, never()).save(mapper.doctorToMasterView(any()));
  }

}
