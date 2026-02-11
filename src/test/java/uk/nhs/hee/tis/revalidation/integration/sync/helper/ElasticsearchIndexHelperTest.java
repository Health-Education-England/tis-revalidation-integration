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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteAliasRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.NoSuchIndexException;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexHelperTest {

  @Mock
  private ElasticsearchClient esClient;

  @Mock
  private ElasticsearchIndicesClient indicesClient;

  @Mock
  private GetIndexResponse getIndexResponse;

  @Mock
  private CreateIndexResponse createIndexResponse;

  @Mock
  private GetMappingResponse getMappingResponse;

  @InjectMocks
  private ElasticsearchIndexHelper helper;

  @Test
  void shouldMakeRequestToGetIndices() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.get((GetIndexRequest) any())).thenReturn(getIndexResponse);

    helper.getIndices("index");

    verify(indicesClient).get((GetIndexRequest) any());
  }

  @Test
  void shouldThrowIOExceptionWhenGetIndices() throws Exception {
    IOException expected = new IOException("expected");
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.get((GetIndexRequest) any())).thenThrow(expected);

    IOException actual = assertThrows(IOException.class, () -> helper.getIndices("index"));
    assertEquals(expected, actual);
  }

  @Test
  void shouldMakeRequestToReindex() throws IOException {
    // The helper calls esClient.reindex(...)
    assertDoesNotThrow(() -> helper.reindex("source", "target"));
    verify(esClient).reindex((ReindexRequest) any());
  }

  @Test
  void shouldThrowSocketTimeoutExceptionWhenReindex() throws Exception {
    SocketTimeoutException expected = new SocketTimeoutException("expected");
    when(esClient.reindex((ReindexRequest) any())).thenThrow(expected);

    SocketTimeoutException actual =
        assertThrows(SocketTimeoutException.class, () -> helper.reindex("index1", "index2"));
    assertEquals(expected, actual);
  }

  @Test
  void shouldMakeRequestToDeleteIndex() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);

    helper.deleteIndex("index");

    verify(indicesClient).delete((DeleteIndexRequest) any());
  }

  @Test
  void shouldThrowNoSuchIndexExceptionWhenDeleteIndexNotFound() throws Exception {
    when(esClient.indices()).thenReturn(indicesClient);

    ElasticsearchException elasticsearchException =
        new ElasticsearchException("index_not_found_exception: expected", null);

    when(indicesClient.delete((DeleteIndexRequest) any())).thenThrow(elasticsearchException);

    assertThrows(NoSuchIndexException.class, () -> helper.deleteIndex("index"));
  }

  @Test
  void shouldThrowElasticsearchExceptionWhenDeleteIndexOtherError() throws Exception {
    when(esClient.indices()).thenReturn(indicesClient);

    ElasticsearchException elasticsearchException =
        new ElasticsearchException("some_other_exception: expected", null);

    when(indicesClient.delete((DeleteIndexRequest) any())).thenThrow(elasticsearchException);

    assertThrows(ElasticsearchException.class, () -> helper.deleteIndex("index"));
  }

  @Test
  void shouldMakeRequestToCreateIndexFromMap() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.create((CreateIndexRequest) any())).thenReturn(createIndexResponse);

    Map<String, Object> mapping = Collections.emptyMap();
    helper.createIndex("index", mapping);

    verify(indicesClient).create((CreateIndexRequest) any());
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenCreateIndexAlreadyExists() throws Exception {
    when(esClient.indices()).thenReturn(indicesClient);

    ElasticsearchException esEx =
        new ElasticsearchException("resource_already_exists_exception: expected", null);

    when(indicesClient.create((CreateIndexRequest) any())).thenThrow(esEx);

    assertThrows(IllegalStateException.class,
        () -> helper.createIndex("index", Collections.emptyMap()));
  }

  @Test
  void shouldMakeRequestToCheckAliasExists() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);

    BooleanResponse resp = mock(BooleanResponse.class);
    when(resp.value()).thenReturn(true);

    when(indicesClient.existsAlias(any(Function.class))).thenReturn(resp);

    boolean result = helper.aliasExists("alias");

    assertTrue(result);
    verify(indicesClient).existsAlias(any(Function.class));
  }

  @Test
  void shouldMakeRequestToAddAliasWithoutFilter() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);

    helper.addAlias("index", "alias");

    verify(indicesClient).updateAliases((UpdateAliasesRequest) any());
  }

  @Test
  void shouldMakeRequestToAddAliasWithFilter() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);

    // Filter must be valid JSON Query DSL; "filter" is not valid JSON and would fail building Query.
    // Use a minimal valid query JSON string:
    String filterJson = "{\"term\":{\"existsInGmc\":true}}";

    helper.addAlias("index", "alias", filterJson);

    verify(indicesClient).updateAliases((UpdateAliasesRequest) any());
  }

  @Test
  void shouldMakeRequestToDeleteAlias() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);

    helper.deleteAlias("index", "alias");

    verify(indicesClient).deleteAlias((DeleteAliasRequest) any());
  }

  @Test
  void shouldThrowNoSuchIndexExceptionWhenDeleteAliasIndexNotFound() throws Exception {
    when(esClient.indices()).thenReturn(indicesClient);

    ElasticsearchException esEx =
        new ElasticsearchException("index_not_found_exception: expected", null);

    when(indicesClient.deleteAlias((DeleteAliasRequest) any())).thenThrow(esEx);

    assertThrows(NoSuchIndexException.class, () -> helper.deleteAlias("index", "alias"));
  }

  @Test
  void shouldMakeRequestToGetMapping() throws IOException {
    when(esClient.indices()).thenReturn(indicesClient);
    when(indicesClient.getMapping((GetMappingRequest) any())).thenReturn(getMappingResponse);

    helper.getMapping("index");

    verify(indicesClient).getMapping((GetMappingRequest) any());
  }

  @Test
  void shouldThrowIOExceptionWhenGetMapping() throws Exception {
    when(esClient.indices()).thenReturn(indicesClient);
    IOException expected = new IOException("expected");
    when(indicesClient.getMapping((GetMappingRequest) any())).thenThrow(expected);

    IOException actual = assertThrows(IOException.class, () -> helper.getMapping("index"));
    assertEquals(expected, actual);
  }
}
