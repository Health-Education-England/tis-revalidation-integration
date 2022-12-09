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
import static org.mockito.Mockito.mock;
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
import org.hamcrest.CoreMatchers;
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

  @Captor
  ArgumentCaptor<String> stringArgCaptor;
  @Mock
  private ElasticsearchIndexHelper elasticsearchIndexHelperMock;
  @Mock
  private GetIndexResponse getIndexResponseMock;
  @Spy
  @InjectMocks
  private ElasticsearchIndexService elasticsearchIndexService;

  @Test
  void shouldDeleteBackupIndices() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("backupAlias")).thenReturn(getIndexResponseMock);
    Settings settings1 = mock(Settings.class);
    Settings settings2 = mock(Settings.class);
    Settings settings3 = mock(Settings.class);
    when(settings1.get("index.creation_date")).thenReturn("1669854215258");
    when(settings2.get("index.creation_date")).thenReturn("1670455701379");
    when(settings3.get("index.creation_date")).thenReturn("1670456912113");
    final String backupIndex1 = "backup_1";
    final String backupIndex2 = "backup_2";
    final String backupIndex3 = "backup_3";
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(backupIndex1, settings1);
    settingsMap.put(backupIndex2, settings2);
    settingsMap.put(backupIndex3, settings3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{backupIndex1, backupIndex2, backupIndex3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest("backupAlias");

    verify(elasticsearchIndexHelperMock, times(2)).deleteIndex(stringArgCaptor.capture());
    List<String> deletedIndices = stringArgCaptor.getAllValues();
    assertThat("Deleted unexpected indices.", deletedIndices,
        CoreMatchers.hasItems(backupIndex1, backupIndex2));
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenCreationTimesAreNull() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("backupAlias")).thenReturn(getIndexResponseMock);
    Settings settings1 = mock(Settings.class);
    Settings settings2 = mock(Settings.class);
    Settings settings3 = mock(Settings.class);
    when(settings1.get("index.creation_date")).thenReturn(null);
    when(settings2.get("index.creation_date")).thenReturn(null);
    when(settings3.get("index.creation_date")).thenReturn(null);
    final String backupIndex1 = "backup_1";
    final String backupIndex2 = "backup_2";
    final String backupIndex3 = "backup_3";
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(backupIndex1, settings1);
    settingsMap.put(backupIndex2, settings2);
    settingsMap.put(backupIndex3, settings3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{backupIndex1, backupIndex2, backupIndex3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest("backupAlias");

    verify(elasticsearchIndexHelperMock, times(0)).deleteIndex(stringArgCaptor.capture());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOnlyOneIndex() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("backupAlias")).thenReturn(getIndexResponseMock);
    Map<String, Settings> settingsMap = new HashMap<>();
    Settings settings1 = mock(Settings.class);

    final String backupIndex1 = "backup_1";
    settingsMap.put(backupIndex1, settings1);
    when(getIndexResponseMock.getIndices()).thenReturn(new String[]{backupIndex1});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest("backupAlias");

    verify(elasticsearchIndexHelperMock, times(0)).deleteIndex(stringArgCaptor.capture());
  }

  @Test
  void shouldNotDeleteBackupIndicesWhenOneHasCreationDateOthersNot() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("backupAlias")).thenReturn(getIndexResponseMock);
    Settings settings1 = mock(Settings.class);
    Settings settings2 = mock(Settings.class);
    Settings settings3 = mock(Settings.class);
    when(settings1.get("index.creation_date")).thenReturn("1669854215258");
    when(settings2.get("index.creation_date")).thenReturn(null);
    when(settings3.get("index.creation_date")).thenReturn(null);
    final String backupIndex1 = "backup_1";
    final String backupIndex2 = "backup_2";
    final String backupIndex3 = "backup_3";
    Map<String, Settings> settingsMap = new HashMap<>();
    settingsMap.put(backupIndex1, settings1);
    settingsMap.put(backupIndex2, settings2);
    settingsMap.put(backupIndex3, settings3);
    when(getIndexResponseMock.getSettings()).thenReturn(settingsMap);
    when(getIndexResponseMock.getIndices()).thenReturn(
        new String[]{backupIndex1, backupIndex2, backupIndex3});

    elasticsearchIndexService.deleteBackupIndicesExceptLatest("backupAlias");

    verify(elasticsearchIndexHelperMock, times(2)).deleteIndex(stringArgCaptor.capture());
    List<String> deletedIndices = stringArgCaptor.getAllValues();
    assertThat("Deleted unexpected indices.", deletedIndices,
        CoreMatchers.hasItems(backupIndex2, backupIndex3));
  }

  @Test
  void shouldTransferOldIndexNameToAlias() throws Exception {
    final String alias = "alias";
    when(elasticsearchIndexHelperMock.getIndices(alias)).thenReturn(getIndexResponseMock);
    Map<String, MappingMetadata> mappingsMap = new HashMap<>();
    MappingMetadata mapping = mock(MappingMetadata.class);
    mappingsMap.put(alias, mapping);
    when(getIndexResponseMock.getMappings()).thenReturn(mappingsMap);

    String returnedBackupName = elasticsearchIndexService.transferOldIndexNameToAlias(alias);
    String backupAlias = elasticsearchIndexService.getBackupAlias(alias);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mapping));
    String oldIndexBackupName = stringArgCaptor.getValue();
    assertEquals(oldIndexBackupName, returnedBackupName);
    verify(elasticsearchIndexHelperMock).reindex(alias, oldIndexBackupName);
    verify(elasticsearchIndexHelperMock).addAlias(oldIndexBackupName, backupAlias);
    verify(elasticsearchIndexHelperMock).deleteIndex(alias);
    verify(elasticsearchIndexHelperMock).addAlias(oldIndexBackupName, alias);
  }

  @Test
  void shouldThrowExceptionWhenNoMappingFoundForTransferOldIndexNameToAlias() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("alias")).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.getMappings()).thenReturn(new HashMap<>());

    assertThrows(ResourceNotFoundException.class,
        () -> elasticsearchIndexService.transferOldIndexNameToAlias("alias"));
    verify(elasticsearchIndexHelperMock, times(0)).createIndex(anyString(), any());
  }

  @Test
  void shouldThrowExceptionWhenNoIndexFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("alias")).thenReturn(getIndexResponseMock);
    when(getIndexResponseMock.getAliases()).thenReturn(new HashMap<>());

    assertThrows(NoSuchElementException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup("alias"));
    verify(elasticsearchIndexHelperMock, times(0)).addAlias(anyString(), anyString());
  }

  @Test
  void shouldThrowExceptionWhenMultipleIndicesFoundForMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("alias")).thenReturn(getIndexResponseMock);
    Map<String, List<AliasMetadata>> aliasMap = new HashMap<>();
    AliasMetadata aliasMetadata1 = mock(AliasMetadata.class);
    AliasMetadata aliasMetadata2 = mock(AliasMetadata.class);
    aliasMap.put("index1", Lists.list(aliasMetadata1));
    aliasMap.put("index2", Lists.list(aliasMetadata2));
    when(getIndexResponseMock.getAliases()).thenReturn(aliasMap);

    assertThrows(IllegalStateException.class,
        () -> elasticsearchIndexService.markCurrentIndexAsBackup("alias"));
    verify(elasticsearchIndexHelperMock, times(0)).addAlias(anyString(), anyString());
  }

  @Test
  void shouldMarkCurrentIndexAsBackup() throws Exception {
    when(elasticsearchIndexHelperMock.getIndices("alias")).thenReturn(getIndexResponseMock);
    Map<String, List<AliasMetadata>> aliasMap = new HashMap<>();
    AliasMetadata aliasMetadata1 = mock(AliasMetadata.class);
    aliasMap.put("index1", Lists.list(aliasMetadata1));
    when(getIndexResponseMock.getAliases()).thenReturn(aliasMap);

    String oldIndexName = elasticsearchIndexService.markCurrentIndexAsBackup("alias");
    verify(elasticsearchIndexHelperMock).addAlias(eq(oldIndexName), anyString());
  }

  @Test
  void shouldReindexWhenAliasNotExists() throws Exception {
    final String sourceIndexName = "sourceIndex";
    final String targetAlias = "targetAlias";

    when(elasticsearchIndexHelperMock.aliasExists(targetAlias)).thenReturn(false);
    final String oldIndexName = "oldIndexName";
    doReturn(oldIndexName).when(elasticsearchIndexService).transferOldIndexNameToAlias(targetAlias);
    MappingMetadata mappingMock = mock(MappingMetadata.class);
    when(elasticsearchIndexHelperMock.getMapping(oldIndexName)).thenReturn(mappingMock);
    doNothing().when(elasticsearchIndexService).deleteBackupIndicesExceptLatest(targetAlias);

    elasticsearchIndexService.resync(sourceIndexName, targetAlias);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mappingMock));
    String newTargetIndexName = stringArgCaptor.getValue();
    verify(elasticsearchIndexHelperMock).reindex(sourceIndexName, newTargetIndexName);
    verify(elasticsearchIndexHelperMock).addAlias(newTargetIndexName, targetAlias);
    verify(elasticsearchIndexHelperMock).deleteAlias(oldIndexName, targetAlias);
  }

  @Test
  void shouldReindexWhenAliasExists() throws Exception {
    final String sourceIndexName = "sourceIndex";
    final String targetAlias = "targetAlias";

    when(elasticsearchIndexHelperMock.aliasExists(targetAlias)).thenReturn(true);
    final String oldIndexName = "oldIndexName";
    doReturn(oldIndexName).when(elasticsearchIndexService).markCurrentIndexAsBackup(targetAlias);
    MappingMetadata mappingMock = mock(MappingMetadata.class);
    when(elasticsearchIndexHelperMock.getMapping(oldIndexName)).thenReturn(mappingMock);
    doNothing().when(elasticsearchIndexService).deleteBackupIndicesExceptLatest(targetAlias);

    elasticsearchIndexService.resync(sourceIndexName, targetAlias);

    verify(elasticsearchIndexHelperMock).createIndex(stringArgCaptor.capture(), eq(mappingMock));
    String newTargetIndexName = stringArgCaptor.getValue();
    verify(elasticsearchIndexHelperMock).reindex(sourceIndexName, newTargetIndexName);
    verify(elasticsearchIndexHelperMock).addAlias(newTargetIndexName, targetAlias);
    verify(elasticsearchIndexHelperMock).deleteAlias(oldIndexName, targetAlias);
  }

  @Test
  void shouldThrowErrorWhenMappingNotFoundForOldIndexWhenReindex() throws Exception {
    final String sourceIndexName = "sourceIndex";
    final String targetAlias = "targetAlias";

    when(elasticsearchIndexHelperMock.aliasExists(targetAlias)).thenReturn(true);
    final String oldIndexName = "oldIndexName";
    doReturn(oldIndexName).when(elasticsearchIndexService).markCurrentIndexAsBackup(targetAlias);
    when(elasticsearchIndexHelperMock.getMapping(oldIndexName)).thenReturn(null);

    assertThrows(ResourceNotFoundException.class,
        () -> elasticsearchIndexService.resync(sourceIndexName, targetAlias));
  }

  @Test
  void shouldIgnoreAlreadyExistsAndDeleteBackupExceptionsWhenReindex() throws Exception {
    final String sourceIndexName = "sourceIndex";
    final String targetAlias = "targetAlias";

    when(elasticsearchIndexHelperMock.aliasExists(targetAlias)).thenReturn(true);
    String oldIndexName = "oldIndexName";
    doReturn(oldIndexName).when(elasticsearchIndexService).markCurrentIndexAsBackup(targetAlias);
    MappingMetadata mappingMock = mock(MappingMetadata.class);
    when(elasticsearchIndexHelperMock.getMapping(oldIndexName)).thenReturn(mappingMock);

    doThrow(ResourceAlreadyExistsException.class).when(elasticsearchIndexHelperMock)
        .createIndex(anyString(), any());
    String backupAlias = elasticsearchIndexService.getBackupAlias(targetAlias);
    doThrow(Exception.class).when(elasticsearchIndexService)
        .deleteBackupIndicesExceptLatest(backupAlias);

    assertDoesNotThrow(() -> elasticsearchIndexService.resync(sourceIndexName, targetAlias));
  }

  @Test
  void shouldGetBackupAliasAsExpected() {
    String backupAlias = elasticsearchIndexService.getBackupAlias("index");
    assertEquals("index_backup", backupAlias);
  }
}
