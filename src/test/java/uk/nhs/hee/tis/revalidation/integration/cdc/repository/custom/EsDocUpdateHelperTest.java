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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.get.GetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class EsDocUpdateHelperTest {

  private static final String INDEX_NAME = "index";
  private static final String DOC_ID = "123";
  private static final Map<String, Object> UPDATES = Map.of(
      "doctorFirstName", "Alice",
      "doctorLastName", "Brown",
      "lastConnectionDateTime", "2025-11-06T10:26:23.049"
  );

  @Mock
  private RestHighLevelClient highLevelClient;

  private EsDocUpdateHelper esDocUpdateHelper;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    esDocUpdateHelper = new EsDocUpdateHelper(highLevelClient, objectMapper);
  }

  @Test
  void testPartialUpdateSuccess() throws IOException {
    // Given
    // Mock GetResult
    var getResult = mock(GetResult.class);
    when(getResult.isExists()).thenReturn(true);
    when(getResult.sourceAsMap()).thenReturn(UPDATES);

    // Mock UpdateResponse
    UpdateResponse updateResponse = mock(UpdateResponse.class);
    when(updateResponse.getGetResult()).thenReturn(getResult);

    // Mock highLevelClient.update
    when(highLevelClient.update(any(UpdateRequest.class), any(RequestOptions.class)))
        .thenReturn(updateResponse);

    // When
    MasterDoctorView result = esDocUpdateHelper.partialUpdate(INDEX_NAME, DOC_ID, UPDATES,
        MasterDoctorView.class);

    // Then
    assertNotNull(result);
    assertEquals("Alice", result.getDoctorFirstName());
    assertEquals("Brown", result.getDoctorLastName());
    assertNotNull(result.getLastConnectionDateTime());
    assertEquals(2025, result.getLastConnectionDateTime().getYear());
    assertEquals(11, result.getLastConnectionDateTime().getMonthValue());
    assertEquals(6, result.getLastConnectionDateTime().getDayOfMonth());

    verify(highLevelClient).update(any(UpdateRequest.class), any(RequestOptions.class));
  }

  @Test
  void testPartialUpdateDocumentNotFound() throws Exception {
    // Mock UpdateResponse with null getResult
    var updateResponse = mock(UpdateResponse.class);
    when(updateResponse.getGetResult()).thenReturn(null);

    when(highLevelClient.update(any(UpdateRequest.class), any(RequestOptions.class)))
        .thenReturn(updateResponse);

    // Expect exception
    EsDocUpdateHelper.EsUpdateException ex = assertThrows(
        EsDocUpdateHelper.EsUpdateException.class,
        () -> esDocUpdateHelper.partialUpdate(INDEX_NAME, DOC_ID, UPDATES, MasterDoctorView.class)
    );

    assertTrue(ex.getMessage().contains("Document not found after update"));
  }

  @Test
  void testPartialUpdateSourceIsNull() throws Exception {
    var getResult = mock(GetResult.class);
    when(getResult.isExists()).thenReturn(true);
    when(getResult.sourceAsMap()).thenReturn(null);

    // Mock UpdateResponse
    var updateResponse = mock(UpdateResponse.class);
    when(updateResponse.getGetResult()).thenReturn(getResult);

    // Mock highLevelClient.update
    when(highLevelClient.update(any(UpdateRequest.class), any(RequestOptions.class)))
        .thenReturn(updateResponse);

    EsDocUpdateHelper.EsUpdateException ex = assertThrows(
        EsDocUpdateHelper.EsUpdateException.class,
        () -> esDocUpdateHelper.partialUpdate(INDEX_NAME, DOC_ID, UPDATES, MasterDoctorView.class)
    );

    assertTrue(ex.getMessage().contains("Updated document source is null"));
  }

  @Test
  void testPartialUpdateIoException() throws Exception {
    when(highLevelClient.update(any(UpdateRequest.class), any(RequestOptions.class)))
        .thenThrow(new IOException("Connection failed"));

    EsDocUpdateHelper.EsUpdateException ex = assertThrows(
        EsDocUpdateHelper.EsUpdateException.class,
        () -> esDocUpdateHelper.partialUpdate(INDEX_NAME, DOC_ID, UPDATES, MasterDoctorView.class)
    );

    assertTrue(ex.getMessage().contains("Failed to update document"));
  }
}
