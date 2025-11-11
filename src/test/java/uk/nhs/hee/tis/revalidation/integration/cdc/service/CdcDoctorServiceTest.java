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

package uk.nhs.hee.tis.revalidation.integration.cdc.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcDoctorServiceTest {

  @InjectMocks
  CdcDoctorService cdcDoctorService;

  @Mock
  MasterDoctorElasticSearchRepository repository;

  @Mock
  EsDocUpdateHelper esUpdateHelper;

  @Mock
  CdcMessagePublisher publisher;

  @Spy
  MasterDoctorViewMapper mapper = (MasterDoctorViewMapper) new MasterDoctorViewMapperImpl();

  @Captor
  ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor;

  @Captor
  ArgumentCaptor<Map<String, Object>> esUpdateDocCaptor;

  private MasterDoctorView masterDoctorView = CdcTestDataGenerator.getTestMasterDoctorView();

  @Test
  void shouldAddNewFieldsIfDoctorDoesNotExist() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(Collections.emptyList());

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctor();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(repository).save(masterDoctorViewCaptor.capture());
    final var savedView = masterDoctorViewCaptor.getValue();
    assertThat(savedView.getGmcReferenceNumber(), is(newDoctor.getGmcReferenceNumber()));
    assertThat(savedView.getDoctorFirstName(), is(newDoctor.getDoctorFirstName()));
    assertThat(savedView.getDoctorLastName(), is(newDoctor.getDoctorLastName()));
    assertThat(savedView.getDesignatedBody(), is(newDoctor.getDesignatedBodyCode()));
    assertThat(savedView.getTisStatus(), is(newDoctor.getDoctorStatus()));
    assertThat(savedView.getSubmissionDate(), is(newDoctor.getSubmissionDate()));
    //New doctor so no TIS fields
    assertNull(savedView.getTcsPersonId());
  }

  @Test
  void shouldUpdateFieldsIfDoctorExistsOnAdd() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctor();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    final var partialUpdateDoc = esUpdateDocCaptor.getValue();
    assertEquals(newDoctor.getDoctorFirstName(), partialUpdateDoc.get("doctorFirstName"));
    assertEquals(newDoctor.getDoctorLastName(), partialUpdateDoc.get("doctorLastName"));
    assertEquals(newDoctor.getGmcReferenceNumber(), partialUpdateDoc.get("gmcReferenceNumber"));
    assertEquals(newDoctor.getSubmissionDate(), partialUpdateDoc.get("submissionDate"));
    assertEquals(newDoctor.getDoctorStatus(), partialUpdateDoc.get("tisStatus"));
    assertEquals(newDoctor.getDesignatedBodyCode(), partialUpdateDoc.get("designatedBody"));
    assertEquals(newDoctor.getAdmin(), partialUpdateDoc.get("admin"));
    assertEquals(newDoctor.getLastUpdatedDate(), partialUpdateDoc.get("lastUpdatedDate"));
    assertEquals(newDoctor.getUnderNotice(), partialUpdateDoc.get("underNotice"));
    assertEquals(newDoctor.getExistsInGmc(), partialUpdateDoc.get("existsInGmc"));
  }

  @Test
  void shouldSetDesignatedBodyCodeToNull() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctorNullDbc();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(esUpdateHelper).partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        esUpdateDocCaptor.capture(), eq(MasterDoctorView.class));

    assertNull(esUpdateDocCaptor.getValue().get("designatedBody"));
  }

  @Test
  void shouldPublishUpdates() {
    when(repository.findByGmcReferenceNumber(any())).thenReturn(List.of(masterDoctorView));
    when(esUpdateHelper.partialUpdate(eq(MASTER_DOCTOR_INDEX), eq(masterDoctorView.getId()),
        anyMap(), eq(MasterDoctorView.class))).thenReturn(masterDoctorView);

    DoctorsForDB newDoctor = CdcTestDataGenerator.getCdcDoctor();
    cdcDoctorService.upsertEntity(newDoctor);

    verify(publisher).publishCdcUpdate(masterDoctorView);
  }
}
