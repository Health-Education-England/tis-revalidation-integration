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

package uk.nhs.hee.tis.revalidation.integration.sync.helper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElasticsearchIndexHelper {

  private final ElasticsearchClient esClient;

  public ElasticsearchIndexHelper(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  /**
   * get all information (mappings/settings/aliases) for an index.
   *
   * @param indexName the index name could be an exact index name or an alias
   * @return getIndexResponse including settings, mappings and aliases for the index
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public GetIndexResponse getIndices(String indexName) throws IOException {
    return esClient.indices().get(g -> g.index(indexName));
  }

  /**
   * Re-index one elasticsearch index into another.
   *
   * @param sourceIndex The elasticsearch index from which the data will be indexed
   * @param targetIndex The elasticsearch index to which the data will be indexed
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void reindex(String sourceIndex, String targetIndex) throws IOException {
    log.info("Reindexing elasticsearch index: {} -> {}.", sourceIndex, targetIndex);

    try {
      esClient.reindex(r -> r
          .source(s -> s.index(sourceIndex))
          .dest(d -> d.index(targetIndex))
          .refresh(true)
          .timeout(Time.of(t -> t.time("10m")))
      );
    } catch (SocketTimeoutException e) {
      log.error(
          "Reindexing from index: {} to index: {} timed out. Consider increasing client timeouts.",
          sourceIndex, targetIndex, e);
      throw e;
    } catch (ElasticsearchException e) {
      log.error("Elasticsearch reindex failed: {} -> {}. {}", sourceIndex, targetIndex,
          e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Delete an elasticsearch index.
   *
   * @param targetIndex The elasticsearch index to be deleted
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void deleteIndex(String targetIndex) throws IOException {
    log.info("Deleting elasticsearch index: {}", targetIndex);

    try {
      esClient.indices().delete(d -> d.index(targetIndex));
    } catch (ElasticsearchException e) {
      if (isIndexNotFound(e)) {
        throw new NoSuchIndexException("Index not found: " + targetIndex, e);
      }
      throw e;
    }
  }

  /**
   * Create an elasticsearch index with custom field mappings and default settings.
   *
   * @param indexName The name of the elasticsearch index to be created
   * @param mappingSource   the desired mapping object
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public CreateIndexResponse createIndex(String indexName, Map<String, Object> mappingSource)
      throws IOException {

    log.info("Creating elasticsearch index: {} with custom mapping (map).", indexName);

    try {
      return esClient.indices().create(c -> c
          .index(indexName)
          .mappings(m -> m.withJson(new StringReader(toJson(mappingSource))))
      );
    } catch (ElasticsearchException e) {
      if (isAlreadyExists(e)) {
        throw new IllegalStateException("Index already exists: " + indexName, e);
      }
      throw e;
    }
  }

  /**
   * Create index directly from TypeMapping.
   */
  public CreateIndexResponse createIndex(String indexName, TypeMapping mapping) throws IOException {
    log.info("Creating elasticsearch index: {} with custom mapping (TypeMapping).", indexName);

    try {
      return esClient.indices().create(c -> c
          .index(indexName)
          .mappings(mapping)
      );
    } catch (ElasticsearchException e) {
      if (isAlreadyExists(e)) {
        throw new IllegalStateException("Index already exists: " + indexName, e);
      }
      throw e;
    }
  }

  /**
   * Check if an alias exists in ES.
   *
   * @param alias the alias to search with
   * @return true or false
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public boolean aliasExists(String alias) throws IOException {
    BooleanResponse resp = esClient.indices().existsAlias(e -> e.name(alias));
    return resp.value();
  }

  /**
   * Add alias to an index.
   *
   * @param indexName index name to add alias for
   * @param aliasName alias to be added
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void addAlias(String indexName, String aliasName) throws IOException {
    addAlias(indexName, aliasName, null);
  }

  /**
   * Add alias to an index.
   *
   * @param indexName index name to add alias for
   * @param aliasName alias to be added
   * @param filter filter expression that applies to this alias
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void addAlias(String indexName, String aliasName, String filter) throws IOException {
    log.info("Adding alias: {} to elasticsearch index: {}.", aliasName, indexName);

    try {
      esClient.indices().updateAliases(u -> u.actions(a -> {
        if (StringUtils.isNotBlank(filter)) {
          Query filterQuery = new Query.Builder()
              .withJson(new StringReader(filter))
              .build();
          return a.add(add -> add.index(indexName).alias(aliasName).filter(filterQuery));
        }
        return a.add(add -> add.index(indexName).alias(aliasName));
      }));
    } catch (ElasticsearchException e) {
      log.error("Failed to add alias {} to index {}: {}", aliasName, indexName, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Delete an alias from an index.
   *
   * @param indexName index name to delete alias from
   * @param aliasName alias to be deleted
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void deleteAlias(String indexName, String aliasName) throws IOException {
    log.info("Deleting alias: {} from elasticsearch index: {}.", aliasName, indexName);

    try {
      esClient.indices().deleteAlias(d -> d.index(indexName).name(aliasName));
    } catch (ElasticsearchException e) {
      if (isIndexNotFound(e)) {
        throw new NoSuchIndexException("Index not found: " + indexName, e);
      }
      throw e;
    }
  }

  /**
   * Get mapping information for an index.
   *
   * @param indexName index name to search with
   * @return MappingMetadata for the specific index
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public TypeMapping getMapping(String indexName) throws IOException {
    GetMappingResponse response = esClient.indices().getMapping(g -> g.index(indexName));
    var indexMapping = response.result().get(indexName);
    return indexMapping != null ? indexMapping.mappings() : null;
  }

  private String toJson(Map<String, Object> map) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
    } catch (Exception e) {
      throw new DataAccessResourceFailureException("Failed to serialize mapping to JSON", e);
    }
  }

  private boolean isIndexNotFound(ElasticsearchException e) {
    return e.getMessage() != null && e.getMessage().contains("index_not_found_exception");
  }

  private boolean isAlreadyExists(ElasticsearchException e) {
    return e.getMessage() != null && e.getMessage().contains("resource_already_exists_exception");
  }
}
