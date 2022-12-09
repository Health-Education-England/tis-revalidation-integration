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

package uk.nhs.hee.tis.revalidation.integration.sync.helper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.DeleteAliasRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElasticsearchIndexHelper {

  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
      .setConnectTimeout(5000)
      .setSocketTimeout(120000)
      .build();

  RestHighLevelClient highLevelClient;

  public ElasticsearchIndexHelper(RestHighLevelClient highLevelClient) {
    this.highLevelClient = highLevelClient;
  }

  /**
   * get all information (mappings/settings/aliases) for an index.
   *
   * @param indexName the index name could be an exact index name or an alias
   * @return getIndexResponse including settings, mappings and aliases for the index
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public GetIndexResponse getIndices(String indexName) throws IOException {
    GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
    return highLevelClient.indices().get(getIndexRequest, RequestOptions.DEFAULT);
  }

  /**
   * Re-index one elasticsearch index into another.
   *
   * @param sourceIndex The elasticsearch index from which the data will be indexed
   * @param targetIndex The elasticsearch index to which the data will be indexed
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void reindex(String sourceIndex, String targetIndex) throws IOException {
    log.info("Reindexing elastic search index: {} to index: {}.", sourceIndex, targetIndex);
    ReindexRequest request = new ReindexRequest()
        .setSourceIndices(sourceIndex)
        .setDestIndex(targetIndex)
        .setTimeout(TimeValue.timeValueMinutes(10))
        .setRefresh(true);
    RequestOptions options = RequestOptions.DEFAULT.toBuilder().setRequestConfig(REQUEST_CONFIG)
        .build();
    try {
      highLevelClient.reindex(request, options);
    } catch (SocketTimeoutException e) {
      log.error(
          "Reindexing from index: {} to index: {} needs more wait time."
              + "Please consider increasing the SocketTimeout.", sourceIndex, targetIndex);
      log.error(e.getMessage());
      throw e;
    }
  }

  /**
   * Delete an elasticsearch index.
   *
   * @param targetIndex The elasticsearch index to be deleted
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   * @throws ResourceNotFoundException when the index does not exist
   */
  public void deleteIndex(String targetIndex) throws IOException, ResourceNotFoundException {
    log.info("Deleting elastic search index: {}", targetIndex);

    DeleteIndexRequest request = new DeleteIndexRequest(targetIndex);
    highLevelClient.indices().delete(request, RequestOptions.DEFAULT);
  }

  /**
   * Create an elasticsearch index with custom field mappings and default settings.
   *
   * @param indexName The name of the elasticsearch index to be created
   * @param mapping   the desired mapping object
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   * @throws ResourceAlreadyExistsException when the index name already exists
   */
  public void createIndex(String indexName, MappingMetadata mapping)
      throws IOException, ResourceAlreadyExistsException {
    log.info("Creating elastic search index: {} with custom mapping.", indexName);

    CreateIndexRequest request = new CreateIndexRequest(indexName)
        .mapping(mapping.getSourceAsMap());
    highLevelClient.indices().create(request, RequestOptions.DEFAULT);
  }

  /**
   * Check if an alias exists in ES.
   *
   * @param alias the alias to search with
   * @return true or false
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public boolean aliasExists(String alias) throws IOException {
    GetAliasesRequest request = new GetAliasesRequest(alias);
    return highLevelClient.indices().existsAlias(request, RequestOptions.DEFAULT);
  }

  /**
   * Add alias to an index.
   *
   * @param indexName index name to add alias for
   * @param aliasName alias to be added
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public void addAlias(String indexName, String aliasName) throws IOException {
    log.info("Adding alias: {} to elastic search index: {}.", aliasName, indexName);

    IndicesAliasesRequest request = new IndicesAliasesRequest();
    AliasActions aliasAction =
        new AliasActions(AliasActions.Type.ADD)
            .index(indexName)
            .alias(aliasName);
    request.addAliasAction(aliasAction);

    highLevelClient.indices().updateAliases(request, RequestOptions.DEFAULT);
  }

  /**
   * Delete an alias from an index.
   *
   * @param indexName index name to delete alias from
   * @param aliasName alias to be deleted
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   * @throws ResourceNotFoundException when the alias does not exist
   */
  public void deleteAlias(String indexName, String aliasName)
      throws IOException, ResourceNotFoundException {
    log.info("Deleting alias: {} from elastic search index: {}.", aliasName, indexName);

    DeleteAliasRequest request = new DeleteAliasRequest(indexName, aliasName);
    highLevelClient.indices().deleteAlias(request, RequestOptions.DEFAULT);
  }

  /**
   * Get mapping information for an index.
   *
   * @param indexName index name to search with
   * @return MappingMetadata for the specific index
   * @throws IOException for any connection timeout, or socket timeout, or other IO exceptions
   */
  public MappingMetadata getMapping(String indexName) throws IOException {
    GetMappingsRequest request = new GetMappingsRequest();
    GetMappingsResponse getMappingsResponse =
        highLevelClient.indices().getMapping(request, RequestOptions.DEFAULT);
    return getMappingsResponse.mappings().get(indexName);
  }
}
