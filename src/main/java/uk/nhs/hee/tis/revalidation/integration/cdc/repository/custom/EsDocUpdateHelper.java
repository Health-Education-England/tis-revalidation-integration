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

import java.util.Map;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Component;

/**
 * EsDocUpdateHelper provides utility methods for updating Elasticsearch documents.
 */
@Component
public class EsDocUpdateHelper {

  private final ElasticsearchOperations esOperations;

  /**
   * Constructs an EsDocUpdateHelper with the given ElasticsearchOperations instance.
   *
   * @param esOperations the ElasticsearchOperations bean used for performing updates and queries
   */
  public EsDocUpdateHelper(ElasticsearchOperations esOperations) {
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
    UpdateQuery query = UpdateQuery.builder(id)
        .withDocument(Document.from(fields))
        .build();

    esOperations.update(query, IndexCoordinates.of(index));
    return esOperations.get(id, clazz, IndexCoordinates.of(index));
  }
}
