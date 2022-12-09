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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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
public class ElasticsearchIndexHelperTest {

  @Mock
  private RestHighLevelClient highLevelClientMock;

  @Mock
  private IndicesClient indicesClientMock;

  @Mock
  private MappingMetadata mappingMock;

  @InjectMocks
  private ElasticsearchIndexHelper helper;

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
  void shouldThrowExceptionWhenReindexSocketTimeout() throws Exception {
    SocketTimeoutException expectedException = new SocketTimeoutException("expected");
    when(highLevelClientMock.reindex(any(ReindexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedException);

    var actual = assertThrows(SocketTimeoutException.class,
        () -> helper.reindex("index1", "index2"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldThrowIOExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.delete(any(DeleteIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIOException)
        .thenThrow(expectedResourceNotFoundException);

    var actual = assertThrows(IOException.class,
        () -> helper.deleteIndex("index"));

    assertEquals(expectedIOException, actual);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.delete(any(DeleteIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceNotFoundException)
        .thenThrow(expectedIOException);

    var actual = assertThrows(ResourceNotFoundException.class,
        () -> helper.deleteIndex("index"));

    assertEquals(expectedResourceNotFoundException, actual);
  }

  @Test
  void shouldThrowIOExceptionWhenCreateIndex() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceAlreadyExistsException expectedResourceAlreadyExistsException = new ResourceAlreadyExistsException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.create(any(CreateIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIOException)
        .thenThrow(expectedResourceAlreadyExistsException);

    var actual = assertThrows(IOException.class,
        () -> helper.createIndex("index", mappingMock));

    assertEquals(expectedIOException, actual);
  }

  @Test
  void shouldThrowResourceAlreadyExistsExceptionWhenDeleteIndex() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceAlreadyExistsException expectedResourceAlreadyExistsException = new ResourceAlreadyExistsException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.create(any(CreateIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceAlreadyExistsException)
        .thenThrow(expectedIOException);

    var actual = assertThrows(ResourceAlreadyExistsException.class,
        () -> helper.createIndex("index", mappingMock));

    assertEquals(expectedResourceAlreadyExistsException, actual);
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

    var actual = assertThrows(IOException.class, () -> helper.addAlias("index", "alias"));
    assertEquals(expectedException, actual);
  }

  @Test
  void shouldThrowIOExceptionWhenDeleteAlias() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.deleteAlias(any(DeleteAliasRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedIOException)
        .thenThrow(expectedResourceNotFoundException);

    var actual = assertThrows(IOException.class,
        () -> helper.deleteAlias("index", "alias"));

    assertEquals(expectedIOException, actual);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenDeleteAlias() throws Exception {
    IOException expectedIOException = new IOException("expected");
    ResourceNotFoundException expectedResourceNotFoundException = new ResourceNotFoundException(
        "expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.deleteAlias(any(DeleteAliasRequest.class), any(RequestOptions.class)))
        .thenThrow(expectedResourceNotFoundException)
        .thenThrow(expectedIOException);

    var actual = assertThrows(ResourceNotFoundException.class,
        () -> helper.deleteAlias("index", "alias"));

    assertEquals(expectedResourceNotFoundException, actual);
  }

  @Test
  void shouldThrowExceptionWhenGetMapping() throws Exception {
    IOException expectedException = new IOException("expected");
    when(highLevelClientMock.indices()).thenReturn(indicesClientMock);
    when(indicesClientMock.getMapping(any(GetMappingsRequest.class),
        any(RequestOptions.class))).thenThrow(
        expectedException);

    var actual = assertThrows(IOException.class, () -> helper.getMapping("index"));
    assertEquals(expectedException, actual);
  }
}
