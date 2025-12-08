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

package uk.nhs.hee.tis.revalidation.integration.sync.listener;

import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.MASTER_DOCTOR_INDEX;
import static uk.nhs.hee.tis.revalidation.integration.config.EsConstant.Indexes.RECOMMENDATION_INDEX;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;

/**
 * Listener for handling ES rebuild gmc sync messages.
 */
@Slf4j
@Service
public class GmcDoctorMessageListener {

  private final DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  private final ElasticsearchIndexService elasticsearchIndexService;
  private final MasterDoctorViewMapper mapper;

  /**
   * The listener to handle gmc doctor elasticsearch sync messages.
   *
   * @param doctorUpsertElasticSearchService the service to upsert doctors to ES
   * @param elasticsearchIndexService        the service to process elasticsearch indices
   * @param mapper                           the class mapping messages to documents
   */
  public GmcDoctorMessageListener(DoctorUpsertElasticSearchService doctorUpsertElasticSearchService,
      ElasticsearchIndexService elasticsearchIndexService,
      MasterDoctorViewMapper mapper) {
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
    this.elasticsearchIndexService = elasticsearchIndexService;
    this.mapper = mapper;
  }

  /**
   * Handles messages to sync gmc doctor data into elasticsearch, and reindexes when complete.
   *
   * @param message the payload from the doctor sync queue including a flag for the end of the sync
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.revalidationsummary.essync.integration}")
  public void getMessage(IndexSyncMessage message) {
    if (message.getSyncEnd() != null && message.getSyncEnd()) {
      log.info("GMC sync completed. Reindexing Recommendations");
      try {
        elasticsearchIndexService.resync(MASTER_DOCTOR_INDEX, RECOMMENDATION_INDEX);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    } else {
      var docs = mapper.fromRevalidationSummaryDtos(message.getPayload());
      doctorUpsertElasticSearchService.populateMasterIndex(docs);
    }
  }
}
