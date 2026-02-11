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

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexState;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.sync.helper.ElasticsearchIndexHelper;

@Slf4j
@Service
public class ElasticsearchIndexService {

  private static final String ALIAS_BACKUP_SUFFIX = "_backup";
  private static final String INDEX_DATETIME_PATTERN = "yyyyMMddHHmmss";

  private final ElasticsearchIndexHelper elasticsearchIndexHelper;

  public ElasticsearchIndexService(ElasticsearchIndexHelper elasticsearchIndexHelper) {
    this.elasticsearchIndexHelper = elasticsearchIndexHelper;
  }

  protected String getBackupAlias(String alias) {
    return alias + ALIAS_BACKUP_SUFFIX;
  }

  /**
   * Delete historical backups but retain the latest one.
   *
   * Backup indices are discovered by querying the backup alias (alias points to indices).
   * @param backupAlias the alias the backup indices marked with
   * @throws Exception any exceptions
   */
  protected void deleteBackupIndicesExceptLatest(String backupAlias) throws Exception {
    log.info("Start deleting old backup indices for alias: {}", backupAlias);

    GetIndexResponse getIndexResponse = elasticsearchIndexHelper.getIndices(backupAlias);
    Map<String, IndexState> states = getIndexResponse.result();

    if (states == null || states.size() <= 1) {
      return;
    }

    // Identify indices with/without creation_date and keep latest by creation_date
    var indicesWithCreationDate = states.entrySet().stream()
        .filter(e -> StringUtils.isNotBlank(getCreationDate(e.getValue())))
        .toList();

    var indicesWithoutCreationDate = states.entrySet().stream()
        .filter(e -> StringUtils.isBlank(getCreationDate(e.getValue())))
        .map(Map.Entry::getKey)
        .toList();

    if (!indicesWithoutCreationDate.isEmpty()) {
      log.warn("Indices do not have a valid creation date setting: {}. Please consider deleting them manually.",
          indicesWithoutCreationDate);
    }

    var latestBackupIndexOpt = indicesWithCreationDate.stream()
        .max(Comparator.comparingLong(e -> Long.parseLong(getCreationDate(e.getValue()))));

    if (latestBackupIndexOpt.isEmpty()) {
      return;
    }

    String latestBackupIndexName = latestBackupIndexOpt.get().getKey();

    for (var entry : indicesWithCreationDate) {
      String indexName = entry.getKey();
      if (!indexName.equals(latestBackupIndexName)) {
        elasticsearchIndexHelper.deleteIndex(indexName);
      }
    }
  }

  /**
   * Index name and alias cannot be the same. If an index exists with the same name as the alias we want,
   * we reindex it to a new "backup" index name, delete the old index, then re-add alias to the backup.
   *
   * @return the created backup index name
   * @param alias this is the existing index name as well as the alias we want to use.
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   * @throws ElasticsearchException for any Elasticsearch exceptions
   */
  protected String transferOldIndexNameToAlias(String alias) throws IOException, ElasticsearchException {
    String oldIndexName = alias;

    // Get mapping of the existing old index
    TypeMapping mapping = elasticsearchIndexHelper.getMapping(oldIndexName);
    if (mapping == null) {
      throw new NoSuchIndexException("ES mapping for old index \"" + oldIndexName + "\" is not found.");
    }

    String oldIndexBackupName = alias + "_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern(INDEX_DATETIME_PATTERN));

    // Create backup index with same mapping
    elasticsearchIndexHelper.createIndex(oldIndexBackupName, mapping);

    // Reindex old -> backup
    elasticsearchIndexHelper.reindex(oldIndexName, oldIndexBackupName);

    // Mark backup alias on the new backup index
    elasticsearchIndexHelper.addAlias(oldIndexBackupName, getBackupAlias(alias));

    // Delete the original index that conflicts with alias name
    elasticsearchIndexHelper.deleteIndex(oldIndexName);

    // Add the alias name pointing to the backup index
    elasticsearchIndexHelper.addAlias(oldIndexBackupName, alias);

    return oldIndexBackupName;
  }

  /**
   * Set backup alias to the current index behind an alias.
   *
   * @param alias the alias the current index marked with
   * @return the old index name behind the alias
   * @throws IOException any IOExceptions
   */
  protected String markCurrentIndexAsBackup(String alias) throws IOException {
    GetIndexResponse getIndexResponse = elasticsearchIndexHelper.getIndices(alias);

    Map<String, IndexState> states = getIndexResponse.result();
    if (states == null || states.isEmpty()) {
      throw new NoSuchElementException("The index with alias: " + alias + " does not exist.");
    }
    if (states.size() > 1) {
      throw new IllegalStateException("Multiple indices are found for alias: " + alias + ".");
    }

    String oldIndexName = states.keySet().iterator().next();
    elasticsearchIndexHelper.addAlias(oldIndexName, getBackupAlias(alias));
    return oldIndexName;
  }

  /**
   * Reindex from an index to another (known as alias).
   *
   * @param sourceIndexName the index name to reindex from
   * @param targetAlias the alias of index to reindex to
   * @throws Exception any exceptions
   */
  public void resync(String sourceIndexName, String targetAlias) throws Exception {

    boolean aliasExists = elasticsearchIndexHelper.aliasExists(targetAlias);

    String oldIndexName;
    if (aliasExists) {
      oldIndexName = markCurrentIndexAsBackup(targetAlias);
    } else {
      // make the name of existing index available for being set as alias
      oldIndexName = transferOldIndexNameToAlias(targetAlias);
    }

    // Use mapping from the old index (the index currently behind alias)
    TypeMapping mapping = elasticsearchIndexHelper.getMapping(oldIndexName);
    if (mapping == null) {
      throw new NoSuchIndexException("ES mapping for old index \"" + oldIndexName + "\" is not found.");
    }

    String newTargetIndexName = targetAlias + "_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern(INDEX_DATETIME_PATTERN));

    try {
      elasticsearchIndexHelper.createIndex(newTargetIndexName, mapping);
    } catch (IllegalStateException e) {
      // in our helper, createIndex throws IllegalStateException for "already exists"
      log.warn("Creating an existing elasticsearch index: {}. Skipped.", newTargetIndexName, e);
    }

    elasticsearchIndexHelper.reindex(sourceIndexName, newTargetIndexName);

    // Point the alias to the new index
    elasticsearchIndexHelper.addAlias(newTargetIndexName, targetAlias);

    // Delete old backup indices (keep latest)
    String backupAlias = getBackupAlias(targetAlias);
    try {
      deleteBackupIndicesExceptLatest(backupAlias);
    } catch (Exception e) {
      log.warn("Deleting old backup indices for alias: {} skipped. Please delete unnecessary backups manually.",
          backupAlias, e);
    }

    // Finally, remove the alias from the old index
    elasticsearchIndexHelper.deleteAlias(oldIndexName, targetAlias);
  }

  private String getCreationDate(IndexState state) {
    if (state == null || state.settings() == null || state.settings().index() == null) {
      return null;
    }
    // settings.index().creationDate() may be present depending on client version/serialization.
    // If null, we return null and treat as missing.
    try {
      return state.settings().index().creationDate().toString();
    } catch (Exception ignored) {
      return null;
    }
  }
}
