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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.DeleteAliasRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexHelperTest {

  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
      .setConnectTimeout(5000)
      .setSocketTimeout(120000)
      .build();
  @Mock
  private RestHighLevelClient highLevelClientMock;

  @Mock
  private IndicesClient indicesClientMock;

  @Mock
  private MappingMetadata mappingMock;

  @InjectMocks
  private ElasticsearchIndexHelper helper;

  @Test
  void shouldMakeRequestToGetIndices() throws IOException {
    helper.getIndices("index");
    verify(highLevelClientMock).indices().get(any(GetIndexRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldThrowExceptionWhenGetIndices() throws Exception {
    IOException expectedException = new IOException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.get(any(GetIndexRequest.class), any(RequestOptions.class))).thenThrow(
        expectedException);

    var actual = assertThrows(IOException.class, () -> helper.getIndices("index"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldMakeRequestToReindex() throws IOException {
    String source = "source";
    String target = "target";
    RequestOptions options = RequestOptions.DEFAULT.toBuilder().setRequestConfig(REQUEST_CONFIG)
        .build();

    helper.reindex(source, target);

    verify(highLevelClientMock).reindex(any(ReindexRequest.class), options);
  }

  @Test
  void shouldThrowExceptionWhenReindexSocketTimeout() throws Exception {
    SocketTimeoutException expectedException = new SocketTimeoutException("expected");
    when(highLevelClientMock.reindex(any(ReindexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedException);

    var actual = assertThrows(SocketTimeoutException.class,
        () -> helper.reindex("index1", "index2"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldMakeRequestToDeleteIndex() throws IOException {
    helper.deleteIndex("index");

    verify(highLevelClientMock).delete(any(DeleteRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldThrowIoExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.delete(any(DeleteIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIoException)
        .thenThrow(expectedResourceNotFoundException);

    var actual = assertThrows(IOException.class,
        () -> helper.deleteIndex("index"));

    assertEquals(expectedIoException, actual);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.delete(any(DeleteIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceNotFoundException)
        .thenThrow(expectedIoException);

    var actual = assertThrows(ResourceNotFoundException.class,
        () -> helper.deleteIndex("index"));

    assertEquals(expectedResourceNotFoundException, actual);
  }

  @Test
  void shouldThrowIoExceptionWhenCreateIndex() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceAlreadyExistsException expectedResourceAlreadyExistsException =
        new ResourceAlreadyExistsException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.create(any(CreateIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIoException)
        .thenThrow(expectedResourceAlreadyExistsException);

    var actual = assertThrows(IOException.class,
        () -> helper.createIndex("index", mappingMock));

    assertEquals(expectedIoException, actual);
  }

  @Test
  void shouldThrowResourceAlreadyExistsExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceAlreadyExistsException expectedResourceAlreadyExistsException =
        new ResourceAlreadyExistsException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.create(any(CreateIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceAlreadyExistsException)
        .thenThrow(expectedIoException);

    var actual = assertThrows(ResourceAlreadyExistsException.class,
        () -> helper.createIndex("index", mappingMock));

    assertEquals(expectedResourceAlreadyExistsException, actual);
  }

  @Test
  void shouldMakeRequestToCreateIndex() throws IOException {
    Map<String, Object> mapping = Collections.emptyMap();

    MappingMetadata testMetaData = new MappingMetadata("type", mapping);
    helper.createIndex("index", testMetaData);

    verify(highLevelClientMock).delete(any(DeleteRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldThrowExceptionWhenCheckAliasExists() throws Exception {
    IOException expectedException = new IOException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.existsAlias(any(GetAliasesRequest.class),
        any(RequestOptions.class))).thenThrow(
        expectedException);

    var actual = assertThrows(IOException.class, () -> helper.aliasExists("index"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldThrowExceptionWhenAddAliasToIndex() throws Exception {
    IOException expectedException = new IOException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.updateAliases(any(IndicesAliasesRequest.class),
        any(RequestOptions.class))).thenThrow(
        expectedException);

    var actual = assertThrows(IOException.class, () -> helper
        .addAlias("index", "alias"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldThrowIoExceptionWhenDeleteAlias() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.deleteAlias(any(DeleteAliasRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIoException)
        .thenThrow(expectedResourceNotFoundException);

    var actual = assertThrows(IOException.class,
        () -> helper.deleteAlias("index", "alias"));

    assertEquals(expectedIoException, actual);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenDeleteAlias() throws Exception {
    IOException expectedIoException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.deleteAlias(any(DeleteAliasRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceNotFoundException)
        .thenThrow(expectedIoException);

    var actual = assertThrows(ResourceNotFoundException.class,
        () -> helper.deleteAlias("index", "alias"));

    assertEquals(expectedResourceNotFoundException, actual);
  }

  @Test
  void shouldMakeRequestToGetMapping() throws IOException {
    helper.getMapping("index");

    verify(highLevelClientMock).indices()
        .getMapping(any(GetMappingsRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldThrowExceptionWhenGetMapping() throws Exception {
    IOException expectedIoException = new IOException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.getMapping(any(GetMappingsRequest.class),
        any(RequestOptions.class))).thenThrow(
        expectedIoException);

    var actual = assertThrows(IOException.class, () -> helper.getMapping("index"));
    assertEquals(expectedIoException, actual);
  }

  @Test
  void shouldMakeRequestToCheckAliasExists() throws IOException {
    helper.aliasExists("index");

    verify(highLevelClientMock).indices()
        .existsAlias(any(GetAliasesRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldMakeRequestToAddAlias() throws IOException {
    helper.addAlias("index", "alias");

    verify(highLevelClientMock).indices()
        .updateAliases(any(IndicesAliasesRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldMakeRequestToAddAliasWithFilter() throws IOException {
    helper.addAlias("index", "alias", "filter");

    verify(highLevelClientMock).indices()
        .updateAliases(any(IndicesAliasesRequest.class), RequestOptions.DEFAULT);
  }

  @Test
  void shouldMakeRequestToDeleteAlias() throws IOException {
    helper.deleteAlias("index", "alias");

    verify(highLevelClientMock).indices()
        .deleteAlias(any(DeleteAliasRequest.class), RequestOptions.DEFAULT);
  }
}
