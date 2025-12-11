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

package uk.nhs.hee.tis.revalidation.integration.sync.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.RECOMMENDATION_INDEX;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;

@ExtendWith(MockitoExtension.class)
class ConnectionLogMessageListenerTest {

  @Captor
  ArgumentCaptor<List<ConnectionLogDto>> payloadArgCaptor;
  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  @Mock
  private ElasticsearchIndexService elasticsearchIndexService;
  @InjectMocks
  private ConnectionLogMessageListener listener;

  @Test
  void shouldTriggerIndexResyncWhenSyncEndIsTrue() throws Exception {
    // given
    IndexSyncMessage<List<ConnectionLogDto>> msg = new IndexSyncMessage<>();
    msg.setSyncEnd(true);

    // when
    listener.receiveConnectionLogMessage(msg);

    // then
    verify(elasticsearchIndexService).resync(MASTER_DOCTOR_INDEX, RECOMMENDATION_INDEX);
    verifyNoInteractions(doctorUpsertElasticSearchService);
  }

  @Test
  void shouldPopulateMasterIndexWhenSyncEndIsFalse() {
    // given
    List<ConnectionLogDto> payload = List.of(new ConnectionLogDto());
    IndexSyncMessage<List<ConnectionLogDto>> msg = new IndexSyncMessage<>();
    msg.setSyncEnd(false);
    msg.setPayload(payload);

    // when
    listener.receiveConnectionLogMessage(msg);

    // then
    verify(doctorUpsertElasticSearchService).populateMasterIndexByConnectionLogs(
        payloadArgCaptor.capture());
    verifyNoInteractions(elasticsearchIndexService);
    assertEquals(payload, payloadArgCaptor.getValue());
  }

  @Test
  void shouldHandleExceptionInResync() throws Exception {
    // given
    IndexSyncMessage<List<ConnectionLogDto>> msg = new IndexSyncMessage<>();
    msg.setSyncEnd(true);

    doThrow(new RuntimeException("resync failed"))
        .when(elasticsearchIndexService)
        .resync(anyString(), anyString());

    // when
    listener.receiveConnectionLogMessage(msg);

    // then
    verify(elasticsearchIndexService).resync(MASTER_DOCTOR_INDEX, RECOMMENDATION_INDEX);
  }
}
