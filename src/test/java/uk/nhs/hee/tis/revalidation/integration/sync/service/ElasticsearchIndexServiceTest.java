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

package uk.nhs.hee.tis.revalidation.integration.sync.service;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexServiceTest {

  private static final String BACKUP_INDEX_1 = "backup_1";
  private static final String BACKUP_INDEX_2 = "backup_2";
  private static final String BACKUP_INDEX_3 = "backup_3";
  private static final String OLD_INDEX_NAME = "oldIndexName";
  private static final String SOURCE_INDEX_NAME = "sourceIndex";
  private static final String TARGET_ALIAS = "targetAlias";
  private static final String ALIAS = "alias";
  private static final String BACKUP_ALIAS = "backupAlias";

  @Captor
  ArgumentCaptor<String> stringArgCaptor;

  @Mock
  private ElasticsearchIndexHelper elasticsearchIndexHelperMock;

  @Mock
  private GetIndexResponse getIndexResponseMock;

  @Mock
  private TypeMapping mappingMock;

  @Spy
  @InjectMocks
  private ElasticsearchIndexService elasticsearchIndexService;

  @Test
  void shouldDeleteBackupIndices() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);

    // Build result map: 3 indices with creation_date values; keep latest (largest)
    Map<String, IndexState> states = new HashMap<>();
    states.put(BACKUP_INDEX_1, indexStateWithCreationDate("1669854215258"));
    states.put(BACKUP_INDEX_2, indexStateWithCreationDate("1670455701379"));
    states.put(BACKUP_INDEX_3, indexStateWithCreationDate("1670456912113")); // latest
    when(getIndexResponseMock.result()).thenReturn(states);

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, times(2)).deleteIndex(stringArgCaptor.capture());
    List<String> deleted = stringArgCaptor.getAllValues();

    assertThat("Deleted unexpected indices.", deleted, hasItems(BACKUP_INDEX_1, BACKUP_INDEX_2));
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenCreationDatesAreNull() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);

    Map<String, IndexState> states = new HashMap<>();
    states.put(BACKUP_INDEX_1, indexStateWithNullCreationDate());
    states.put(BACKUP_INDEX_2, indexStateWithNullCreationDate());
    states.put(BACKUP_INDEX_3, indexStateWithNullCreationDate());
    when(getIndexResponseMock.result()).thenReturn(states);

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, never()).deleteIndex(anyString());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOnlyOneIndex() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);

    Map<String, IndexState> states = new HashMap<>();
    states.put(BACKUP_INDEX_1, mock(IndexState.class)); // no stubbing needed
    when(getIndexResponseMock.result()).thenReturn(states);

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, never()).deleteIndex(anyString());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOneHasCreationDateOthersNot() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);

    Map<String, IndexState> states = new HashMap<>();
    states.put(BACKUP_INDEX_1, indexStateWithCreationDate("1669854215258"));
    states.put(BACKUP_INDEX_2, indexStateWithNullCreationDate());
    states.put(BACKUP_INDEX_3, indexStateWithNullCreationDate());
    when(getIndexResponseMock.result()).thenReturn(states);

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    // Only one index has creationDate -> it's "latest" among those with creationDate -> no deletions.
    verify(elasticsearchIndexHelperMock, never()).deleteIndex(anyString());
  }

  @Test
  void shouldTransferOldIndexNameToAlias() throws Exception {
    when(elasticsearchIndexHelperMock.getMapping(ALIAS)).thenReturn(mappingMock);

    String returnedBackupName = elasticsearchIndexService.transferOldIndexNameToAlias(ALIAS);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(mappingMock));
    String backupIndexName = stringArgCaptor.getValue();

    assertEquals(backupIndexName, returnedBackupName);
    verify(elasticsearchIndexHelperMock).reindex(ALIAS, backupIndexName);

    String backupAlias = elasticsearchIndexService.getBackupAlias(ALIAS);
    verify(elasticsearchIndexHelperMock).addAlias(backupIndexName, backupAlias);
    verify(elasticsearchIndexHelperMock).deleteIndex(ALIAS);
    verify(elasticsearchIndexHelperMock).addAlias(backupIndexName, ALIAS);
  }

  @Test
  void shouldThrowExceptionWhenNoMappingFoundForTransferOldIndexNameToAlias() throws Exception {
    when(elasticsearchIndexHelperMock.getMapping(ALIAS)).thenReturn(null);

    assertThrows(NoSuchIndexException.class,
        () -> elasticsearchIndexService.transferOldIndexNameToAlias(ALIAS));

    verify(elasticsearchIndexHelperMock, never()).createIndex(anyString(),
        org.mockito.ArgumentMatchers.any(TypeMapping.class));
  }

  @Test
  void shouldThrowExceptionWhenNoIndexFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.result()).thenReturn(new HashMap<>());

    assertThrows(NoSuchElementException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS));

    verify(elasticsearchIndexHelperMock, never()).addAlias(anyString(), anyString());
  }

  @Test
  void shouldThrowExceptionWhenMultipleIndicesFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);

    Map<String, IndexState> states = new HashMap<>();
    states.put("index1", mock(IndexState.class));
    states.put("index2", mock(IndexState.class));
    when(getIndexResponseMock.result()).thenReturn(states);

    assertThrows(IllegalStateException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS));

    verify(elasticsearchIndexHelperMock, never()).addAlias(anyString(), anyString());
  }

  @Test
  void shouldMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);

    Map<String, IndexState> states = new HashMap<>();
    states.put("index1", mock(IndexState.class)); // no stubbing needed
    when(getIndexResponseMock.result()).thenReturn(states);

    String oldIndexName = elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS);

    assertEquals("index1", oldIndexName);

    ArgumentCaptor<String> indexCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> aliasCaptor = ArgumentCaptor.forClass(String.class);

    verify(elasticsearchIndexHelperMock).addAlias(indexCaptor.capture(), aliasCaptor.capture());

    assertEquals("index1", indexCaptor.getValue());
    assertEquals(elasticsearchIndexService.getBackupAlias(ALIAS), aliasCaptor.getValue());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReindexWhetherAliasExists(boolean aliasExists) throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(aliasExists);

    lenient().doReturn(OLD_INDEX_NAME)
        .when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);

    lenient().doReturn(OLD_INDEX_NAME)
        .when(elasticsearchIndexService).transferOldIndexNameToAlias(TARGET_ALIAS);

    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(mappingMock);

    String backupAlias = elasticsearchIndexService.getBackupAlias(TARGET_ALIAS);
    doNothing().when(elasticsearchIndexService).deleteBackupIndicesExceptLatest(backupAlias);

    elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS);

    ArgumentCaptor<String> newIndexCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<TypeMapping> mappingCaptor = ArgumentCaptor.forClass(TypeMapping.class);

    verify(elasticsearchIndexHelperMock).createIndex(newIndexCaptor.capture(),
        mappingCaptor.capture());
    String newTargetIndexName = newIndexCaptor.getValue();
    assertEquals(mappingMock, mappingCaptor.getValue());

    verify(elasticsearchIndexHelperMock).reindex(SOURCE_INDEX_NAME, newTargetIndexName);
    verify(elasticsearchIndexHelperMock).addAlias(newTargetIndexName, TARGET_ALIAS);
    verify(elasticsearchIndexHelperMock).deleteAlias(OLD_INDEX_NAME, TARGET_ALIAS);
  }

  @Test
  void shouldThrowErrorWhenMappingNotFoundForOldIndexWhenReindex() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(true);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);

    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(null);

    assertThrows(NoSuchIndexException.class,
        () -> elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS));
  }

  @Test
  void shouldIgnoreAlreadyExistsAndDeleteBackupExceptionsWhenReindex() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(true);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);
    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(mappingMock);

    // helper.createIndex throws IllegalStateException when already exists (per your helper)
    doThrow(new IllegalStateException("resource_already_exists_exception"))
        .when(elasticsearchIndexHelperMock)
        .createIndex(anyString(), org.mockito.ArgumentMatchers.any(TypeMapping.class));

    String backupAlias = elasticsearchIndexService.getBackupAlias(TARGET_ALIAS);
    doThrow(new Exception("delete backups failed"))
        .when(elasticsearchIndexService)
        .deleteBackupIndicesExceptLatest(backupAlias);

    assertDoesNotThrow(() -> elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS));
  }

  @Test
  void shouldGetBackupAliasAsExpected() {
    String backupAlias = elasticsearchIndexService.getBackupAlias("index");
    assertEquals("index_backup", backupAlias);
  }

  private IndexState indexStateWithCreationDate(String creationDate) {
    // Deep stub so we can call: state.settings().index().creationDate().toString()
    IndexState state = mock(IndexState.class, RETURNS_DEEP_STUBS);
    when(state.settings().index().creationDate()).thenReturn(Long.valueOf(creationDate));
    return state;
  }

  private IndexState indexStateWithNullCreationDate() {
    IndexState state = mock(IndexState.class, RETURNS_DEEP_STUBS);
    when(state.settings().index().creationDate()).thenReturn(null);
    return state;
  }
}
