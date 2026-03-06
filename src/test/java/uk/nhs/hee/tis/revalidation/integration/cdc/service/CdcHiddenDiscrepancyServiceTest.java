/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (NHS England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;

import java.util.Collections;
import java.util.Map;
import org.elasticsearch.common.collect.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcHiddenDiscrepancyServiceTest {

  private final MasterDoctorView masterDoctorView = CdcTestDataGenerator
      .getTestMasterDoctorView();
  private final MasterDoctorView masterDoctorViewWithHidden = CdcTestDataGenerator
      .getTestMasterDoctorViewWithHidden();
  private final MasterDoctorView masterDoctorViewWithMultipleHidden = CdcTestDataGenerator
      .getTestMasterDoctorViewWithMultipleHidden();
  @InjectMocks
  @Spy
  CdcHiddenDiscrepancyService cdcHiddenDiscrepancyService;
  @Mock
  MasterDoctorElasticSearchRepository repository;
  @Mock
  EsDocUpdateHelper esUpdateHelper;
  @Mock
  CdcMessagePublisher publisher;
  @Captor
  ArgumentCaptor<Map<String, Object>> esUpdateDocCaptor;

  private static final String HIDDEN_DISCREPANCIES_KEY = "hiddenDiscrepancies";

  @Test
  void shouldAddNewField() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var newHiddenDiscrepancy = CdcTestDataGenerator.getCdcHiddenDiscrepancyInsertCdcDocumentDto();
    cdcHiddenDiscrepancyService.upsertEntity(newHiddenDiscrepancy.getFullDocument());

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    assertThat(esUpdateDocCaptor.getValue().keySet().stream().findFirst().get(),
        is(HIDDEN_DISCREPANCIES_KEY));
    assertThat(esUpdateDocCaptor.getValue().get(HIDDEN_DISCREPANCIES_KEY),
        is(List.of(newHiddenDiscrepancy.getFullDocument())));
  }

  @Test
  void shouldAddRecordToExistingField() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(masterDoctorViewWithHidden));

    var newHiddenDiscrepancy = CdcTestDataGenerator
        .getSecondHiddenDiscrepancyInsertCdcDocumentDto();
    cdcHiddenDiscrepancyService.upsertEntity(newHiddenDiscrepancy.getFullDocument());

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    assertThat(esUpdateDocCaptor.getValue().keySet().stream().findFirst().get(),
        is(HIDDEN_DISCREPANCIES_KEY));
    assertThat(esUpdateDocCaptor.getValue().get(HIDDEN_DISCREPANCIES_KEY),
        is(List.of(masterDoctorViewWithHidden.getHiddenDiscrepancies().get(0),
            newHiddenDiscrepancy.getFullDocument())));
  }

  @Test
  void shouldNotInsertRecordIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    var newConnectionLog = CdcTestDataGenerator.getCdcHiddenDiscrepancyInsertCdcDocumentDto();
    cdcHiddenDiscrepancyService.upsertEntity(newConnectionLog.getFullDocument());

    verify(esUpdateHelper, never()).partialUpdate(eq(MASTER_DOCTOR_INDEX),
        eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class));
  }

  @Test
  void shouldRemoveRecordsFromOnDeleteOperation() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var newHiddenDiscrepancy = CdcTestDataGenerator.getCdcHiddenDiscrepancyDeleteCdcDocumentDto();
    cdcHiddenDiscrepancyService.deleteEntity(newHiddenDiscrepancy.getFullDocument());

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    assertThat(esUpdateDocCaptor.getValue().keySet().stream().findFirst().get(),
        is(HIDDEN_DISCREPANCIES_KEY));
    assertThat(esUpdateDocCaptor.getValue().get(HIDDEN_DISCREPANCIES_KEY),
        is(Collections.emptyList()));
  }

  @Test
  void shouldRemoveIndividualRecordsFromFieldOnDeleteOperation() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(masterDoctorViewWithMultipleHidden));

    var deletedHiddenDiscrepancy = CdcTestDataGenerator
        .getCdcHiddenDiscrepancyDeleteCdcDocumentDto();
    cdcHiddenDiscrepancyService.deleteEntity(deletedHiddenDiscrepancy.getFullDocument());

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    assertThat(esUpdateDocCaptor.getValue().keySet().stream().findFirst().get(),
        is(HIDDEN_DISCREPANCIES_KEY));
    assertThat(esUpdateDocCaptor.getValue().get(HIDDEN_DISCREPANCIES_KEY),
        is(List.of(masterDoctorViewWithMultipleHidden.getHiddenDiscrepancies().get(1))));
  }

  @Test
  void shouldNotAddSecondRecordIfAlreadyHiddenForDesignatedBodyAndGmcReferenceNumber() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(
        List.of(masterDoctorViewWithHidden));

    var newConnectionLog = CdcTestDataGenerator.getCdcHiddenDiscrepancyInsertCdcDocumentDto();
    cdcHiddenDiscrepancyService.upsertEntity(newConnectionLog.getFullDocument());

    verify(esUpdateHelper, never()).partialUpdate(eq(MASTER_DOCTOR_INDEX),
        eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class));
  }
}
