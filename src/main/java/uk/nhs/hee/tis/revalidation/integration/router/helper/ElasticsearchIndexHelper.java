package uk.nhs.hee.tis.revalidation.integration.router.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElasticsearchIndexHelper {

  RestHighLevelClient highLevelClient;

  public ElasticsearchIndexHelper(RestHighLevelClient highLevelClient) {
    this.highLevelClient = highLevelClient;
  }

  /**
   * Re-index one elasticsearch index into another.
   *
   * @param sourceIndex The elasticsearch index from which the data will be indexed
   * @param targetIndex The elasticsearch index to which the data will be indexed
   */
  public void reindex(String sourceIndex, String targetIndex) throws IOException {
    ReindexRequest request = new ReindexRequest()
        .setSourceIndices(sourceIndex)
        .setDestIndex(targetIndex);

    highLevelClient.reindex(request, RequestOptions.DEFAULT);
  }

  /**
   * Delete an elasticsearch index.
   *
   * @param targetIndex The elasticsearch index to be deleted
   */
  public void deleteIndex(String targetIndex) throws IOException {
    log.info("Deleting elastic search index: {}", targetIndex);

    try {
      DeleteIndexRequest request = new DeleteIndexRequest(targetIndex);
      highLevelClient.indices().delete(request, RequestOptions.DEFAULT);
    } catch (ElasticsearchException exception) {
      log.info("Could not delete an index that does not exist: {}", targetIndex);
    }
  }

  /**
   * Create an elasticsearch index with default field mappings and settings.
   *
   * @param indexName The name of the elasticsearch index to be created
   */
  public void createIndex(String indexName) throws IOException {
    log.info("Creating elastic search index: {}", indexName);

    CreateIndexRequest request = new CreateIndexRequest(indexName);
    highLevelClient.indices().create(request, RequestOptions.DEFAULT);
  }

  /**
   * Create an elasticsearch index with custom field mappings and default settings.
   *
   * @param indexName The name of the elasticsearch index to be created
   * @param mapping A Json string of the desired mapping
   */
  public void createIndex(String indexName, String mapping) throws IOException {
    log.info("Creating elastic search index: {} with custom mapping", indexName);
    Map<String, Object> indexMapping = (HashMap<String,Object>) new ObjectMapper()
        .readValue(mapping, HashMap.class);

    CreateIndexRequest request = new CreateIndexRequest(indexName)
        .mapping(indexMapping);
    highLevelClient.indices().create(request, RequestOptions.DEFAULT);
  }

}
