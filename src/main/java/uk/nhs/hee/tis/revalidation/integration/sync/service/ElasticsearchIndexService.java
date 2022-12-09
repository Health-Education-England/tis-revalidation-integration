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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;

@Slf4j
@Service
public class ElasticsearchIndexService {

  private static final String ALIAS_BACKUP_SUFFIX = "_backup";
  private static final String INDEX_DATETIME_PATTERN = "yyyyMMddHHmmss";

  ElasticsearchIndexHelper elasticsearchIndexHelper;

  public ElasticsearchIndexService(
      ElasticsearchIndexHelper elasticsearchIndexHelper) {
    this.elasticsearchIndexHelper = elasticsearchIndexHelper;
  }

  protected String getBackupAlias(String alias) {
    return alias + ALIAS_BACKUP_SUFFIX;
  }

  /**
   * Delete historical backups but retain the latest one.
   *
   * @param backupAlias the alias the backup indices marked with
   * @throws Exception any exceptions
   */
  protected void deleteBackupIndicesExceptLatest(String backupAlias) throws Exception {
    log.info("Start deleting old backup indices for alias: {}", backupAlias);
    GetIndexResponse getIndexResponse = elasticsearchIndexHelper.getIndices(backupAlias);
    if (getIndexResponse.getIndices().length <= 1) {
      return;
    }
    Long latest = null;
    String latestBackupIndex = null;
    for (var entry : getIndexResponse.getSettings().entrySet()) {
      String epochCreationTime = entry.getValue().get("index.creation_date");
      if (epochCreationTime != null) {
        Long epochLong = Long.valueOf(epochCreationTime);
        if (latest == null || epochLong.compareTo(latest) > 0) {
          latest = epochLong;
          latestBackupIndex = entry.getKey();
        }
      }
    }

    if (latestBackupIndex != null) {
      for (String index : getIndexResponse.getIndices()) {
        if (!index.equals(latestBackupIndex)) {
          elasticsearchIndexHelper.deleteIndex(index);
        }
      }
    }
  }

  /**
   * Index name and alias can not be the same. To use an existing index name as an alias, we need to
   * reindex the index to another name, and delete it, then set alias back to it.
   *
   * @param alias this is the existing index name as well as the alias we want to use.
   * @throws Exception any exceptions
   */
  protected String transferOldIndexNameToAlias(String alias) throws Exception {
    String oldIndexName = alias;
    GetIndexResponse getIndexResponse = elasticsearchIndexHelper.getIndices(oldIndexName);
    MappingMetadata mapping = getIndexResponse.getMappings().get(oldIndexName);
    String oldIndexBackupName = alias + "_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern(INDEX_DATETIME_PATTERN));
    if (mapping == null) {
      throw new ResourceNotFoundException(
          String.format("ES mapping for old index \"%s\" is not found.", oldIndexName));
    }
    elasticsearchIndexHelper.createIndex(oldIndexBackupName, mapping);
    elasticsearchIndexHelper.reindex(oldIndexName, oldIndexBackupName);
    elasticsearchIndexHelper.addAlias(oldIndexBackupName, getBackupAlias(alias));
    elasticsearchIndexHelper.deleteIndex(oldIndexName);
    elasticsearchIndexHelper.addAlias(oldIndexBackupName, alias);
    return oldIndexBackupName;
  }

  /**
   * Set backup alias to the current index.
   *
   * @param alias the alias the current index marked with
   * @return the old index name which marked with the alias
   * @throws IOException any IOExceptions
   */
  protected String markCurrentIndexAsBackup(String alias) throws IOException {
    GetIndexResponse getIndexResponse = elasticsearchIndexHelper.getIndices(alias);
    var indexMap = getIndexResponse.getAliases();

    if (indexMap.isEmpty()) {
      throw new NoSuchElementException(
          String.format("The index with alias: %s does not exist.", alias));
    } else if (indexMap.size() > 1) {
      throw new IllegalStateException(
          String.format("Multiple indices are found for alias: %s.", alias));
    }
    String oldIndexName = getIndexResponse.getAliases().keySet().iterator().next();
    elasticsearchIndexHelper.addAlias(oldIndexName, getBackupAlias(alias));
    return oldIndexName;
  }

  /**
   * reindex from an index to another (known as alias).
   *
   * @param sourceIndexName the index name to reindex from
   * @param targetAlias the alias of index to reindex to
   * @throws Exception any exceptions
   */
  public void resync(String sourceIndexName, String targetAlias) throws Exception {

    boolean aliasExists = elasticsearchIndexHelper.aliasExists(targetAlias);

    String oldIndexName;
    if (!aliasExists) {
      // and make the name of existing index available for being set as alias
      oldIndexName = transferOldIndexNameToAlias(targetAlias);
    } else {
      oldIndexName = markCurrentIndexAsBackup(targetAlias);
    }

    MappingMetadata mapping = elasticsearchIndexHelper.getMapping(oldIndexName);
    if (mapping == null) {
      throw new ResourceNotFoundException(
          String.format("ES mapping for old index \"%s\" is not found.", oldIndexName));
    }
    String newTargetIndexName =
        targetAlias + "_" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern(INDEX_DATETIME_PATTERN));
    try {
      elasticsearchIndexHelper.createIndex(newTargetIndexName, mapping);
    } catch (ResourceAlreadyExistsException e) {
      log.warn("Creating an existing elastic search index: {}. Skipped. Exception: {}",
          newTargetIndexName, e);
    }
    elasticsearchIndexHelper.reindex(sourceIndexName, newTargetIndexName);
    elasticsearchIndexHelper.addAlias(newTargetIndexName, targetAlias);

    String backupAlias = getBackupAlias(targetAlias);
    try {
      deleteBackupIndicesExceptLatest(backupAlias);
    } catch (Exception e) {
      log.warn(
          "Deleting old backup indices for alias: {}. skipped."
              + "Please delete unnecessary backups manually. Exception: {}",
          backupAlias, e);
    }
    // Finally, remove the alias from the old index
    elasticsearchIndexHelper.deleteAlias(oldIndexName, targetAlias);
  }
}
