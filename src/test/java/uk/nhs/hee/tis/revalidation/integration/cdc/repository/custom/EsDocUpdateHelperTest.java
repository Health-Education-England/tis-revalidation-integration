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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.get.GetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
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
  private static final String GMC_NUMBER = "101";
  private static final String FIRST_NAME = "AAA";
  private static final String LAST_NAME = "BBB";
  private static final LocalDate SUBMISSION_DATE = LocalDate.now();
  private static final UnderNotice UNDER_NOTICE = UnderNotice.NO;
  private static final RecommendationStatus RECOMMENDATION_STATUS
      = RecommendationStatus.NOT_STARTED;
  private static final LocalDate LAST_UPDATED = LocalDate.now();
  private static final String DESIGNATED_BODY_CODE = "PQR";
  private static final String ADMIN = "Reval Admin";
  private static final boolean EXISTS_IN_GMC = true;
  private static final RecommendationGmcOutcome OUTCOME = RecommendationGmcOutcome.UNDER_REVIEW;

  @Mock
  private RestHighLevelClient highLevelClient;

  @Mock
  private ElasticsearchOperations elasticsearchOperations;

  private EsDocUpdateHelper esDocUpdateHelper;

  @Captor
  ArgumentCaptor<List<UpdateQuery>> bulkUpdateCaptor;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    esDocUpdateHelper = new EsDocUpdateHelper(highLevelClient, objectMapper,
        elasticsearchOperations);
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

  @Test
  void shouldBulkUpdate() {
    Map<String, Object> map = new HashMap<>();
    // Map fields explicitly
    map.put("doctorFirstName", FIRST_NAME);
    map.put("doctorLastName", LAST_NAME);
    map.put("gmcReferenceNumber", GMC_NUMBER);
    map.put("submissionDate", SUBMISSION_DATE);
    map.put("tisStatus", RECOMMENDATION_STATUS);
    map.put("designatedBody", DESIGNATED_BODY_CODE);
    map.put("admin", ADMIN);
    map.put("lastUpdatedDate", LAST_UPDATED);
    map.put("underNotice", UNDER_NOTICE);
    map.put("existsInGmc", EXISTS_IN_GMC);
    map.put("gmcStatus", OUTCOME);

    Map<String, Map<String, Object>> mapById = new HashMap<>();
    mapById.put("123", map);

    var indexCoords = IndexCoordinates.of("index");

    esDocUpdateHelper.bulkPartialUpdate("index", mapById);

    verify(elasticsearchOperations).bulkUpdate(bulkUpdateCaptor.capture(), eq(indexCoords));

    var bulkUpdateRequests = bulkUpdateCaptor.getValue();
    assertEquals(1, bulkUpdateRequests.size());

    var bulkUpdate = bulkUpdateRequests.get(0).getDocument();
    assertEquals(FIRST_NAME, bulkUpdate.get("doctorFirstName"));
    assertEquals(LAST_NAME, bulkUpdate.get("doctorLastName"));
    assertEquals(GMC_NUMBER, bulkUpdate.get("gmcReferenceNumber"));
    assertEquals(SUBMISSION_DATE, bulkUpdate.get("submissionDate"));
    assertEquals(RECOMMENDATION_STATUS, bulkUpdate.get("tisStatus"));
    assertEquals(DESIGNATED_BODY_CODE, bulkUpdate.get("designatedBody"));
    assertEquals(ADMIN, bulkUpdate.get("admin"));
    assertEquals(LAST_UPDATED, bulkUpdate.get("lastUpdatedDate"));
    assertEquals(UNDER_NOTICE, bulkUpdate.get("underNotice"));
    assertEquals(EXISTS_IN_GMC, bulkUpdate.get("existsInGmc"));
    assertEquals(OUTCOME, bulkUpdate.get("gmcStatus"));
  }

}
