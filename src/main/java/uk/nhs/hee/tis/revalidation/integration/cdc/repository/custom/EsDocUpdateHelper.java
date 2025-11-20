/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Component;

/**
 * EsDocUpdateHelper provides utility methods for updating Elasticsearch documents.
 */
@Slf4j
@Component
public class EsDocUpdateHelper {

  private static final int DEFAULT_RETRY_ON_CONFLICT = 5;

  private final RestHighLevelClient highLevelClient;

  private final ObjectMapper objectMapper;

  private final ElasticsearchOperations esOperations;

  /**
   * Constructs an EsDocUpdateHelper with the given ElasticsearchOperations instance.
   *
   * @param highLevelClient the Elasticsearch high-level client used to execute update operations.
   * @param objectMapper    the object mapper used to convert the Elasticsearch
   *                        response {@code updatedMap} into the corresponding Java entity.
   */
  public EsDocUpdateHelper(RestHighLevelClient highLevelClient,
      ObjectMapper objectMapper, ElasticsearchOperations esOperations) {
    this.highLevelClient = highLevelClient;
    this.objectMapper = objectMapper;
    this.esOperations = esOperations;
  }

  /**
   * Performs a partial update on a document in the specified Elasticsearch index.
   *
   * <p>Only the fields provided in the {@code fields} map will be updated, leaving
   * other fields intact. After the update, the method retrieves and returns the
   * updated document.
   *
   * @param index the name of the Elasticsearch index where the document resides
   * @param id the unique identifier of the document to update
   * @param fields a map of field names and their new values to be updated
   * @param clazz the class type of the document to be returned
   * @param <T> the type of the document
   * @return the updated document of type {@code T} after the partial update
   */
  public <T> T partialUpdate(String index, String id, Map<String, Object> fields, Class<T> clazz) {
    UpdateRequest request = new UpdateRequest(index, id)
        .doc(fields)
        .fetchSource(true)
        .retryOnConflict(DEFAULT_RETRY_ON_CONFLICT);

    try {
      UpdateResponse response = highLevelClient.update(request, RequestOptions.DEFAULT);

      if (response.getGetResult() == null || !response.getGetResult().isExists()) {
        throw new EsUpdateException(
            "Document not found after update. Index: " + index + ", ID: " + id);
      }

      Map<String, Object> updatedMap = response.getGetResult().sourceAsMap();
      if (updatedMap == null) {
        throw new EsUpdateException(
            "Updated document source is null. Index: " + index + ", ID: " + id);
      }

      return objectMapper.convertValue(updatedMap, clazz);

    } catch (IOException e) {
      log.error("Failed to update document in index {} with ID {}: {}", index, id, e.getMessage(),
          e);
      throw new EsUpdateException("Failed to update document. Index: " + index + ", ID: " + id, e);
    }
  }

  /**
   * A custom runtime exception indicating a failure during an Elasticsearch update operation.
   */
  public static class EsUpdateException extends RuntimeException {

    public EsUpdateException(String message) {
      super(message);
    }

    public EsUpdateException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Performs a bulk partial update of documents in the specified Elasticsearch index.
   *
   * <p>Only the fields provided in the {@code fieldsById} map will be updated, leaving
   * other fields intact.
   *
   * @param index      the name of the Elasticsearch index where the document resides
   * @param fieldsById a map of fields to update, mapped by documentId
   */
  public void bulkPartialUpdate(String index, Map<String, Map<String, Object>> fieldsById) {
    List<UpdateQuery> queries = new ArrayList<>();

    for (var entry : fieldsById.entrySet()) {
      queries.add(UpdateQuery.builder(entry.getKey())
          .withDocument(Document.from(entry.getValue()))
          .build());
    }

    esOperations.bulkUpdate(queries, IndexCoordinates.of(index));
  }
}
