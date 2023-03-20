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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class DoctorUpsertElasticSearchServiceTest {
  private static final String ES_INDEX = "masterdoctorindex";
  private static final String CURRENT_CONNECTIONS_ALIAS = "current_connections";
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
  @Captor
  private ArgumentCaptor<IndexCoordinates> indexCaptor;
  private DoctorUpsertElasticSearchService service;
  private MasterDoctorView currentDoctorView;
  private MasterDoctorView dataToSave;
  private MasterDoctorView mappedView;

  @BeforeEach
  void setUp() {
    service = new DoctorUpsertElasticSearchService(
        repository, mapper, elasticsearchOperations, elasticsearchIndexHelper);
    currentDoctorView = MasterDoctorView.builder()
        .id("1a2b3c")
        .tcsPersonId(1001L)
        .gmcReferenceNumber("56789")
        .doctorFirstName("doctorFirstName")
        .doctorLastName("doctorLastName")
        .build();

    dataToSave = MasterDoctorView.builder()
        .doctorFirstName("doctorFirstName")
        .doctorLastName("doctorLastName_new")
        .build();

    mappedView = MasterDoctorView.builder()
        .id("1a2b3c")
        .tcsPersonId(1001L)
        .gmcReferenceNumber("56789")
        .doctorFirstName("doctorFirstName_new")
        .doctorLastName("doctorLastName_new")
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
    indexCaptor.getAllValues().forEach(i -> assertEquals("masterdoctorindex", i.getIndexName()));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithGmcIdAndPersonId() {
    // set dataToSave with TcsPersonId and GmcReferenceNumber
    dataToSave.setTcsPersonId(1001L);
    dataToSave.setGmcReferenceNumber("56789");

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
    dataToSave.setGmcReferenceNumber("56789");

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
    dataToSave.setTcsPersonId(1001L);

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
    dataToSave.setGmcReferenceNumber("12345");

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
    verify(elasticsearchIndexHelper).addAlias(ES_INDEX, CURRENT_CONNECTIONS_ALIAS);
  }

  @Test
  void shouldThrowIOExceptionWhenFailingToAddAliasToMasterDoctorIndex() throws IOException {
    IOException expectedException = new IOException("expected");
    doThrow(expectedException).when(elasticsearchIndexHelper).addAlias(ES_INDEX, CURRENT_CONNECTIONS_ALIAS);
    assertThrows(IOException.class, () -> service.clearMasterDoctorIndex());
  }
}
