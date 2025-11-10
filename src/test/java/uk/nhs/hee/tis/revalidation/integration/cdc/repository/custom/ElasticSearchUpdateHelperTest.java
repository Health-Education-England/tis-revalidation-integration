/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright 2025 Crown Copyright (Health Education England)
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  * associated documentation files (the "Software"), to deal in the Software without restriction,
 *  * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *  * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or
 *  * substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 *  * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package uk.nhs.hee.tis.revalidation.integration.cdc.repository.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class ElasticSearchUpdateHelperTest {

  private static final String INDEX_NAME = "index";
  private static final String DOC_ID = "123";
  private static final Map<String, Object> UPDATES = Map.of(
      "doctorFirstName", "Alice",
      "doctorLastName", "Brown"
  );

  @Mock
  private ElasticsearchOperations esOperations;

  @InjectMocks
  private ElasticSearchUpdateHelper esUpdateHelper;

  @Test
  void testPartialUpdate() {
    // Given
    // Mock esOperations.get
    MasterDoctorView updatedView = new MasterDoctorView();
    updatedView.setDoctorFirstName("Alice");
    updatedView.setDoctorLastName("Brown");
    when(esOperations.get(DOC_ID, MasterDoctorView.class, IndexCoordinates.of(INDEX_NAME)))
        .thenReturn(updatedView);

    // Mock esOperations.update
    UpdateResponse mockResponse = mock(UpdateResponse.class);
    when(esOperations.update(any(UpdateQuery.class), eq(IndexCoordinates.of(INDEX_NAME)))).thenReturn(
        mockResponse);

    // when
    MasterDoctorView result = esUpdateHelper.partialUpdate(INDEX_NAME, DOC_ID, UPDATES,
        MasterDoctorView.class);

    // Then
    assertNotNull(result);
    assertEquals("Alice", result.getDoctorFirstName());
    assertEquals("Brown", result.getDoctorLastName());

    verify(esOperations, times(1)).update(any(UpdateQuery.class), eq(IndexCoordinates.of(INDEX_NAME)));
    verify(esOperations, times(1)).get(DOC_ID, MasterDoctorView.class, IndexCoordinates.of(INDEX_NAME));
  }
}
