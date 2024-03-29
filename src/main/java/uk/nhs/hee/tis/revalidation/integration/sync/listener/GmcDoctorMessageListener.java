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

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class GmcDoctorMessageListener {

  private final DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  private final ElasticsearchIndexService elasticsearchIndexService;

  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  private long traineeCount;

  public GmcDoctorMessageListener(DoctorUpsertElasticSearchService doctorUpsertElasticSearchService,
      ElasticsearchIndexService elasticsearchIndexService) {
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
    this.elasticsearchIndexService = elasticsearchIndexService;
  }

  @SqsListener(value = "${cloud.aws.end-point.uri}")
  public void getMessage(IndexSyncMessage<RevalidationSummaryDto> message) {
    if (message.getSyncEnd() != null && message.getSyncEnd()) {
      log.info("GMC sync completed. {} trainees in total. Reindexing Recommendations",
          traineeCount);
      try {
        elasticsearchIndexService.resync("masterdoctorindex", "recommendationindex");
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      traineeCount = 0;
    } else {
      //prepare the MasterDoctorView and call the service method
      final var doctorsForDB = message.getPayload().getDoctor();
      MasterDoctorView masterDoctorView = MasterDoctorView.builder()
          .gmcReferenceNumber(doctorsForDB.getGmcReferenceNumber())
          .doctorFirstName(doctorsForDB.getDoctorFirstName())
          .doctorLastName(doctorsForDB.getDoctorLastName())
          .submissionDate(doctorsForDB.getSubmissionDate())
          .designatedBody(doctorsForDB.getDesignatedBodyCode())
          .gmcStatus(message.getPayload().getGmcOutcome())
          .tisStatus(message.getPayload().getDoctor().getDoctorStatus())
          .admin(doctorsForDB.getAdmin())
          .lastUpdatedDate(doctorsForDB.getLastUpdatedDate())
          .underNotice(doctorsForDB.getUnderNotice())
          .existsInGmc(doctorsForDB.getExistsInGmc())
          .build();
      doctorUpsertElasticSearchService.populateMasterIndex(masterDoctorView);
      traineeCount++;
    }
  }
}
