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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.assertj.core.util.Lists;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
  @Spy
  @InjectMocks
  private ElasticsearchIndexService elasticsearchIndexService;
  @Mock
  private Settings settingsMock1, settingsMock2, settingsMock3;
  @Mock
  private MappingMetadata mappingMock;
  @Mock
  private AliasMetadata aliasMetadataMock1, aliasMetadataMock2;

  @Test
  void shouldDeleteBackupIndices() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);
    when(settingsMock1.get("index.creation_date")).thenReturn("1669854215258");
    when(settingsMock2.get("index.creation_date")).thenReturn("1670455701379");
    when(settingsMock3.get("index.creation_date")).thenReturn("1670456912113");
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(BACKUP_INDEX_1, settingsMock1);
    settingsMap.put(BACKUP_INDEX_2, settingsMock2);
    settingsMap.put(BACKUP_INDEX_3, settingsMock3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{BACKUP_INDEX_1, BACKUP_INDEX_2, BACKUP_INDEX_3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, times(2)).deleteIndex(stringArgCaptor.capture());
    List<String> deletedIndices = stringArgCaptor.getAllValues();
    assertThat("Deleted unexpected indices.", deletedIndices,
        hasItems(BACKUP_INDEX_1, BACKUP_INDEX_2));
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenCreationDatesAreNull() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);
    when(settingsMock1.get("index.creation_date")).thenReturn(null);
    when(settingsMock2.get("index.creation_date")).thenReturn(null);
    when(settingsMock3.get("index.creation_date")).thenReturn(null);
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(BACKUP_INDEX_1, settingsMock1);
    settingsMap.put(BACKUP_INDEX_2, settingsMock2);
    settingsMap.put(BACKUP_INDEX_3, settingsMock3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{BACKUP_INDEX_1, BACKUP_INDEX_2, BACKUP_INDEX_3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, never()).deleteIndex(stringArgCaptor.capture());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOnlyOneIndex() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.getIndices()).thenReturn(new String[]{BACKUP_INDEX_1});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, never()).deleteIndex(stringArgCaptor.capture());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOneHasCreationDateOthersNot() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(BACKUP_ALIAS)).thenReturn(getIndexResponseMock);
    when(settingsMock1.get("index.creation_date")).thenReturn("1669854215258");
    when(settingsMock2.get("index.creation_date")).thenReturn(null);
    when(settingsMock3.get("index.creation_date")).thenReturn(null);
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(BACKUP_INDEX_1, settingsMock1);
    settingsMap.put(BACKUP_INDEX_2, settingsMock2);
    settingsMap.put(BACKUP_INDEX_3, settingsMock3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{BACKUP_INDEX_1, BACKUP_INDEX_2, BACKUP_INDEX_3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest(BACKUP_ALIAS);

    verify(elasticsearchIndexHelperMock, never()).deleteIndex(stringArgCaptor.capture());
  }

  @Test
  void shouldTransferOldIndexNameToAlias() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    Map<String, MappingMetadata> mappingsMap = new HashMap<>();
    mappingsMap.put(ALIAS, mappingMock);
    when(getIndexResponseMock.getMappings()).thenReturn(mappingsMap);

    String returnedBackupName = elasticsearchIndexService.transferOldIndexNameToAlias(ALIAS);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mappingMock));
    String oldIndexBackupName = stringArgCaptor.getValue();
    assertEquals(oldIndexBackupName, returnedBackupName);
    verify(elasticsearchIndexHelperMock).reindex(ALIAS, oldIndexBackupName);
    String backupAlias = elasticsearchIndexService.getBackupAlias(ALIAS);
    verify(elasticsearchIndexHelperMock).addAlias(oldIndexBackupName, backupAlias);
    verify(elasticsearchIndexHelperMock).deleteIndex(ALIAS);
    verify(elasticsearchIndexHelperMock).addAlias(oldIndexBackupName, ALIAS);
  }

  @Test
  void shouldThrowExceptionWhenNoMappingFoundForTransferOldIndexNameToAlias() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.getMappings()).thenReturn(new HashMap<>());

    assertThrows(ResourceNotFoundException.class,
        () -> elasticsearchIndexService.transferOldIndexNameToAlias(ALIAS));
    verify(elasticsearchIndexHelperMock, never()).createIndex(anyString(), any());
  }

  @Test
  void shouldThrowExceptionWhenNoIndexFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.getAliases()).thenReturn(new HashMap<>());

    assertThrows(NoSuchElementException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS));
    verify(elasticsearchIndexHelperMock, never()).addAlias(anyString(), anyString());
  }

  @Test
  void shouldThrowExceptionWhenMultipleIndicesFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    Map<String, List<AliasMetadata>> aliasMap = new HashMap<>();
    aliasMap.put("index1", Lists.list(aliasMetadataMock1));
    aliasMap.put("index2", Lists.list(aliasMetadataMock2));
    when(getIndexResponseMock.getAliases()).thenReturn(aliasMap);

    assertThrows(IllegalStateException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS));
    verify(elasticsearchIndexHelperMock, never()).addAlias(anyString(), anyString());
  }

  @Test
  void shouldMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices(ALIAS)).thenReturn(getIndexResponseMock);
    Map<String, List<AliasMetadata>> aliasMap = new HashMap<>();
    aliasMap.put("index1", Lists.list(aliasMetadataMock1));
    when(getIndexResponseMock.getAliases()).thenReturn(aliasMap);

    String oldIndexName = elasticsearchIndexService.markCurrentIndexAsBackup(ALIAS);
    verify(elasticsearchIndexHelperMock).addAlias(eq(oldIndexName), anyString());
  }

  @Test
  void shouldReindexWhenAliasNotExists() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(false);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService)
        .transferOldIndexNameToAlias(TARGET_ALIAS);
    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(mappingMock);
    doNothing().when(elasticsearchIndexService).deleteBackupIndicesExceptLatest(TARGET_ALIAS);

    elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mappingMock));
    String newTargetIndexName = stringArgCaptor.getValue();
    verify(elasticsearchIndexHelperMock).reindex(SOURCE_INDEX_NAME, newTargetIndexName);
    verify(elasticsearchIndexHelperMock).addAlias(newTargetIndexName, TARGET_ALIAS);
    verify(elasticsearchIndexHelperMock).deleteAlias(OLD_INDEX_NAME, TARGET_ALIAS);
  }

  @Test
  void shouldReindexWhetherAliasExists() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(true);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);
    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(mappingMock);
    doNothing().when(elasticsearchIndexService).deleteBackupIndicesExceptLatest(TARGET_ALIAS);

    elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mappingMock));
    String newTargetIndexName = stringArgCaptor.getValue();
    verify(elasticsearchIndexHelperMock).reindex(SOURCE_INDEX_NAME, newTargetIndexName);
    verify(elasticsearchIndexHelperMock).addAlias(newTargetIndexName, TARGET_ALIAS);
    verify(elasticsearchIndexHelperMock).deleteAlias(OLD_INDEX_NAME, TARGET_ALIAS);
  }

  @Test
  void shouldThrowErrorWhenMappingNotFoundForOldIndexWhenReindex() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(true);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);
    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(null);

    assertThrows(ResourceNotFoundException.class,
        () -> elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS));
  }

  @Test
  void shouldIgnoreAlreadyExistsAndDeleteBackupExceptionsWhenReindex() throws Exception {
    when(elasticsearchIndexHelperMock.aliasExists(TARGET_ALIAS)).thenReturn(true);
    doReturn(OLD_INDEX_NAME).when(elasticsearchIndexService).markCurrentIndexAsBackup(TARGET_ALIAS);
    when(elasticsearchIndexHelperMock.getMapping(OLD_INDEX_NAME)).thenReturn(mappingMock);

    doThrow(ResourceAlreadyExistsException.class).when(elasticsearchIndexHelperMock)
        .createIndex(anyString(), any());
    String backupAlias = elasticsearchIndexService.getBackupAlias(TARGET_ALIAS);
    doThrow(Exception.class).when(elasticsearchIndexService)
        .deleteBackupIndicesExceptLatest(backupAlias);

    assertDoesNotThrow(() -> elasticsearchIndexService.resync(SOURCE_INDEX_NAME, TARGET_ALIAS));
  }

  @Test
  void shouldGetBackupAliasAsExpected() {
    String backupAlias = elasticsearchIndexService.getBackupAlias("index");
    assertEquals("index_backup", backupAlias);
  }
}
