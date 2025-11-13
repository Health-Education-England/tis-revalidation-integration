/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcConnectionService.ES_DATETIME_FORMATTER;
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
class CdcConnectionServiceTest {

  private final MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();
  @InjectMocks
  @Spy
  CdcConnectionService cdcConnectionService;
  @Mock
  MasterDoctorElasticSearchRepository repository;
  @Mock
  EsDocUpdateHelper esUpdateHelper;
  @Mock
  CdcMessagePublisher publisher;
  @Captor
  ArgumentCaptor<Map<String, Object>> esUpdateDocCaptor;

  @Test
  void shouldAddNewFields() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class));
  }

  @Test
  void shouldNotInsertRecordIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(esUpdateHelper, never()).partialUpdate(eq(MASTER_DOCTOR_INDEX),
        eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class));
  }

  @Test
  void shouldPublishUpdates() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(esUpdateHelper.partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class))).thenReturn(masterDoctorView);

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionService.upsertEntity(newConnectionLog.getFullDocument());

    verify(publisher).publishCdcUpdate(masterDoctorView);
  }

  @Test
  void shouldProvideCorrectConnectionLogValue() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(esUpdateHelper.partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class))).thenReturn(masterDoctorView);

    var newConnectionLog = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto()
        .getFullDocument();
    cdcConnectionService.upsertEntity(newConnectionLog);

    final var partialUpdateDoc = esUpdateDocCaptor.getValue();
    assertEquals(newConnectionLog.getUpdatedBy(), partialUpdateDoc.get("updatedBy"));
    assertEquals(newConnectionLog.getRequestTime().format(ES_DATETIME_FORMATTER),
        partialUpdateDoc.get("lastConnectionDateTime"));
  }
}
