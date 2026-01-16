/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.sync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Aliases.CURRENT_CONNECTIONS_ALIAS;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Aliases.DISCREPANCIES_ALIAS;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Aliases.RECOMMENDATION_ALIAS;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;
import static uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService.ES_DATETIME_FORMATTER;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom.EsDocUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class DoctorUpsertElasticSearchServiceTest {

  private final List<MasterDoctorView> recordsAlreadyInEs = new ArrayList<>();
  @Mock
  ElasticsearchOperations elasticsearchOperations;
  @Mock
  private MasterDoctorElasticSearchRepository repository;
  @Mock
  private MasterDoctorViewMapper mapper;
  @Mock
  private IndexOperations indexOperations;
  @Mock
  private ElasticsearchIndexHelper elasticsearchIndexHelper;
  @Mock
  private EsDocUpdateHelper esDocUpdateHelper;
  @Mock
  private RabbitTemplate rabbitTemplate;
  @Captor
  private ArgumentCaptor<IndexCoordinates> indexCaptor;
  @Captor
  private ArgumentCaptor<List<MasterDoctorView>> bulkSaveCaptor;
  @Captor
  private ArgumentCaptor<Map<String, Map<String, Object>>> bulkUpdateCaptor;
  @Captor
  private ArgumentCaptor<String> routingKeyCaptor;
  @Captor
  private ArgumentCaptor<List<MasterDoctorView>> updateListCaptor;
  @InjectMocks
  private DoctorUpsertElasticSearchService service;
  private MasterDoctorView currentDoctorView;
  private MasterDoctorView dataToSave;
  private MasterDoctorView mappedView;
  private MasterDoctorView mappedNewViewGmcOnly;
  private MasterDoctorView mappedExistingViewGmcOnly;
  private static final String UPDATED_BY = "User";

  private static final String GMC_NUMBER = "1111111";
  private static final String GMC_NUMBER_KEY = "gmcReferenceNumber";
  private static final String DOCUMENT_ID = "1";
  private static final Long TIS_ID = 1111111L;
  private static final String TIS_ID_KEY = "tisPersonId";
  private static final String DOCTOR_FIRST_NAME = "firstName";
  private static final String DOCTOR_FIRST_NAME_KEY = "doctorFirstName";
  private static final String DOCTOR_FIRST_NAME_NEW = "firstName_new";
  private static final String DOCTOR_LAST_NAME = "lastName";
  private static final String DOCTOR_LAST_NAME_KEY = "doctorLastName";
  private static final String DOCTOR_LAST_NAME_NEW = "lastName_new";
  private static final LocalDateTime LAST_CONNECTION_DATETIME = LocalDateTime.now().minusDays(1);
  private ConnectionLogDto connectionLogDto;

  @BeforeEach
  void setUp() {
    currentDoctorView = MasterDoctorView.builder()
        .id(DOCUMENT_ID)
        .tcsPersonId(TIS_ID)
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME)
        .doctorLastName(DOCTOR_LAST_NAME)
        .build();

    dataToSave = MasterDoctorView.builder()
        .doctorFirstName(DOCTOR_FIRST_NAME)
        .doctorLastName(DOCTOR_LAST_NAME_NEW)
        .build();

    mappedView = MasterDoctorView.builder()
        .id(DOCUMENT_ID)
        .tcsPersonId(TIS_ID)
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME_NEW)
        .doctorLastName(DOCTOR_LAST_NAME_NEW)
        .build();

    mappedNewViewGmcOnly = MasterDoctorView.builder()
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME_NEW)
        .doctorLastName(DOCTOR_LAST_NAME_NEW)
        .build();

    mappedExistingViewGmcOnly = MasterDoctorView.builder()
        .id(DOCUMENT_ID)
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME_NEW)
        .doctorLastName(DOCTOR_LAST_NAME_NEW)
        .build();

    connectionLogDto = ConnectionLogDto.builder()
        .gmcId(GMC_NUMBER)
        .updatedBy(UPDATED_BY)
        .eventDateTime(LAST_CONNECTION_DATETIME)
        .build();

    // prepare existing record in ES Master
    recordsAlreadyInEs.add(currentDoctorView);
  }

  @Test
  void shouldIgnoreNotFoundOnDeleteButThrowIndexNotFoundAfterCreate() {
    IndexNotFoundException expectedException = new IndexNotFoundException("expected");
    when(elasticsearchOperations.indexOps((IndexCoordinates) any()))
        .thenThrow(new IndexNotFoundException("Index"))
        .thenReturn(indexOperations)
        .thenThrow(expectedException);

    var actual = assertThrows(IndexNotFoundException.class, () -> service.clearMasterDoctorIndex());

    assertEquals(expectedException, actual);
    verify(elasticsearchOperations, times(3)).indexOps((IndexCoordinates) any());
    verify(indexOperations).create();
  }

  @Test
  void shouldDeleteAndAddIndexWithMappings() {
    when(elasticsearchOperations.indexOps(indexCaptor.capture())).thenReturn(indexOperations);
    service.clearMasterDoctorIndex();

    verify(elasticsearchOperations, times(3)).indexOps((IndexCoordinates) any());
    indexCaptor.getAllValues().forEach(i -> assertEquals(MASTER_DOCTOR_INDEX, i.getIndexName()));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithGmcIdAndPersonId() {
    // set dataToSave with TcsPersonId and GmcReferenceNumber
    dataToSave.setTcsPersonId(TIS_ID);
    dataToSave.setGmcReferenceNumber(GMC_NUMBER);

    // find es index by GmcReferenceNumber and TcsPersonId will return and existing record
    when(repository.findByGmcReferenceNumberAndTcsPersonId(dataToSave.getGmcReferenceNumber(),
        dataToSave.getTcsPersonId())).thenReturn(recordsAlreadyInEs);
    when(mapper.updateMasterDoctorView(dataToSave, currentDoctorView)).thenReturn(mappedView);

    service.populateMasterIndex(dataToSave);

    // should update index with mappedView
    verify(repository).save(mappedView);
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithGmcId() {
    // set dataToSave with GmcReferenceNumber
    dataToSave.setGmcReferenceNumber(GMC_NUMBER);

    // find es index by GmcReferenceNumber will return and existing record
    when(repository.findByGmcReferenceNumber(dataToSave.getGmcReferenceNumber()))
        .thenReturn(recordsAlreadyInEs);
    when(mapper.updateMasterDoctorView(dataToSave, currentDoctorView)).thenReturn(mappedView);

    service.populateMasterIndex(dataToSave);

    // should update index with mappedView
    verify(repository).save(mappedView);
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithPersonId() {
    // set dataToSave with TcsPersonId
    dataToSave.setTcsPersonId(TIS_ID);

    // find es index by TcsPersonId will return and existing record
    when(repository.findByTcsPersonId(dataToSave.getTcsPersonId())).thenReturn(recordsAlreadyInEs);
    when(mapper.updateMasterDoctorView(dataToSave, currentDoctorView)).thenReturn(mappedView);

    service.populateMasterIndex(dataToSave);

    // should update index with mappedView
    verify(repository).save(mappedView);
  }

  @Test
  void shouldAddMasterDoctorViewsWhenRecordIsNotInEs() {
    // set dataToSave with a different GmcReferenceNumber
    dataToSave.setGmcReferenceNumber(GMC_NUMBER);

    // find es index by GmcReferenceNumber don't return any existing record
    when(repository.findByGmcReferenceNumber(dataToSave.getGmcReferenceNumber()))
        .thenReturn(Collections.emptyList());

    service.populateMasterIndex(dataToSave);

    // should save index with dataToSave
    verify(repository).save(dataToSave);
  }

  @Test
  void shouldAddCurrentConnectionsAliasToMasterDoctorIndex() throws IOException {
    when(elasticsearchOperations.indexOps((IndexCoordinates) any())).thenReturn(indexOperations);
    service.clearMasterDoctorIndex();
    verify(elasticsearchIndexHelper).addAlias(MASTER_DOCTOR_INDEX, CURRENT_CONNECTIONS_ALIAS,
        DoctorUpsertElasticSearchService.ES_CURRENT_CONNECTIONS_FILTER);
  }

  @Test
  void shouldAddDiscrepanciesAliasToMasterDoctorIndex() throws IOException {
    when(elasticsearchOperations.indexOps((IndexCoordinates) any())).thenReturn(indexOperations);
    service.clearMasterDoctorIndex();
    verify(elasticsearchIndexHelper).addAlias(MASTER_DOCTOR_INDEX, DISCREPANCIES_ALIAS,
        DoctorUpsertElasticSearchService.ES_DISCREPANCIES_FILTER);
  }

  @Test
  void shouldAddRecommendationsAliasToMasterDoctorIndex() throws IOException {
    when(elasticsearchOperations.indexOps((IndexCoordinates) any())).thenReturn(indexOperations);
    service.clearMasterDoctorIndex();
    verify(elasticsearchIndexHelper).addAlias(MASTER_DOCTOR_INDEX, RECOMMENDATION_ALIAS,
        DoctorUpsertElasticSearchService.ES_RECOMMENDATION_FILTER);
  }

  @Test
  void shouldBulkSaveNewDoctors() {
    when(repository.findByGmcReferenceNumber(
        mappedNewViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        List.of());

    service.populateMasterIndex(List.of(mappedNewViewGmcOnly));

    verify(repository).saveAll(bulkSaveCaptor.capture());
    var savedDoctor = bulkSaveCaptor.getValue().get(0);
    assertEquals(savedDoctor.getDoctorFirstName(), mappedNewViewGmcOnly.getDoctorFirstName());
    assertEquals(savedDoctor.getDoctorLastName(), mappedNewViewGmcOnly.getDoctorLastName());
    assertEquals(savedDoctor.getGmcReferenceNumber(), mappedNewViewGmcOnly.getGmcReferenceNumber());
    assertEquals(savedDoctor.getDoctorFirstName(), mappedNewViewGmcOnly.getDoctorFirstName());
  }

  @Test
  void shouldBulkUpdateExistingDoctors() {
    when(repository.findByGmcReferenceNumber(
        mappedExistingViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        recordsAlreadyInEs);

    service.populateMasterIndex(List.of(mappedExistingViewGmcOnly));

    verify(esDocUpdateHelper).bulkPartialUpdate(eq(MASTER_DOCTOR_INDEX),
        bulkUpdateCaptor.capture());

    Map<String, Object> savedFields = Map.of();
    String updatedId = "";
    for (var entry : bulkUpdateCaptor.getValue().entrySet()) {
      savedFields = entry.getValue();
      updatedId = entry.getKey();
    }

    assertEquals(savedFields.get(DOCTOR_FIRST_NAME_KEY),
        mappedExistingViewGmcOnly.getDoctorFirstName());
    assertEquals(savedFields.get(DOCTOR_LAST_NAME_KEY),
        mappedExistingViewGmcOnly.getDoctorLastName());
    assertEquals(savedFields.get(GMC_NUMBER_KEY),
        mappedExistingViewGmcOnly.getGmcReferenceNumber());
    assertEquals(DOCUMENT_ID, updatedId);
    assertFalse(savedFields.containsKey(TIS_ID_KEY)); // Fields from TIS/TCS not updated
  }

  @Test
  void shouldNotBulkSaveNewDoctorsIfNoNewDoctors() {
    when(repository.findByGmcReferenceNumber(
        mappedExistingViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        recordsAlreadyInEs);

    service.populateMasterIndex(List.of(mappedExistingViewGmcOnly));

    verify(repository, never()).saveAll(any());
  }

  @Test
  void shouldNotBulkUpdateNewDoctorsIfNoNewDoctors() {
    when(repository.findByGmcReferenceNumber(
        mappedNewViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        List.of());

    service.populateMasterIndex(List.of(mappedNewViewGmcOnly));

    verify(esDocUpdateHelper, never()).bulkPartialUpdate(any(), any());
  }

  @Test
  void shouldBulkUpdateExistingDoctorsWithConnectionLogs() {
    when(repository.findByGmcReferenceNumber(
        mappedExistingViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        recordsAlreadyInEs);

    service.populateMasterIndexByConnectionLogs(List.of(connectionLogDto));

    verify(esDocUpdateHelper).bulkPartialUpdate(eq(MASTER_DOCTOR_INDEX),
        bulkUpdateCaptor.capture());

    Map<String, Object> savedFields = Map.of();
    String updatedId = "";
    for (var entry : bulkUpdateCaptor.getValue().entrySet()) {
      savedFields = entry.getValue();
      updatedId = entry.getKey();
    }

    assertEquals(savedFields.get("updatedBy"),
        connectionLogDto.getUpdatedBy());
    assertEquals(savedFields.get("lastConnectionDateTime"),
        connectionLogDto.getEventDateTime().format(ES_DATETIME_FORMATTER));
    assertEquals(DOCUMENT_ID, updatedId);
    assertFalse(savedFields.containsKey(TIS_ID_KEY)); // Fields from TIS/TCS not updated
    assertFalse(savedFields.containsKey(GMC_NUMBER_KEY)); // Fields from Recommendation not updated
  }

  @Test
  void shouldNotBulkUpdateNewDoctorsWithConnectionLogsIfNoExistingDoctors() {
    when(repository.findByGmcReferenceNumber(
        mappedNewViewGmcOnly.getGmcReferenceNumber())).thenReturn(
        List.of());

    service.populateMasterIndexByConnectionLogs(List.of(connectionLogDto));

    verify(esDocUpdateHelper, never()).bulkPartialUpdate(any(), any());
  }

  @Test
  void shouldHandleEmptyConnectionLogList() {
    service.populateMasterIndexByConnectionLogs(Collections.emptyList());
    verify(esDocUpdateHelper, never()).bulkPartialUpdate(any(), any());
  }
}
