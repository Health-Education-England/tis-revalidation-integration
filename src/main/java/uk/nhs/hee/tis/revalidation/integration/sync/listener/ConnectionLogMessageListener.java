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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionLogDto;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;

/**
 * Listener for connection log messages from RabbitMQ to sync data into Elasticsearch.
 */
@Slf4j
@Service
public class ConnectionLogMessageListener {

  private final DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  /**
   * Constructor for the ConnectionLogMessageListener.
   *
   * @param doctorUpsertElasticSearchService the service to upsert doctors in Elasticsearch
   */
  public ConnectionLogMessageListener(
      DoctorUpsertElasticSearchService doctorUpsertElasticSearchService) {
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
  }

  /**
   * Receives connection log messages from RabbitMQ and processes them for Elasticsearch sync.
   *
   * @param message the index sync message containing connection log data
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.connectionlog.essyncdata}")
  public void receiveConnectionLogMessage(IndexSyncMessage<List<ConnectionLogDto>> message) {
    if (message.getSyncEnd() != null && message.getSyncEnd()) {
      log.info("ConnectionLogs ES sync completed.");
    } else {
      doctorUpsertElasticSearchService.populateMasterIndexByConnectionLogs(message.getPayload());
    }
  }
}
