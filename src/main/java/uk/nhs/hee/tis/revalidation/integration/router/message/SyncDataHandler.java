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
 *
*/

package uk.nhs.hee.tis.revalidation.integration.router.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Component
public class SyncDataHandler {

  private ObjectMapper objectMapper;

  DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  public SyncDataHandler(DoctorUpsertElasticSearchService doctorUpsertElasticSearchService) {
    objectMapper = new ObjectMapper();
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
  }


  /**
   * Updates Master ElasticSearch index with data from connection sync data router.
   *
   * @param exchange routed exchanged containing ConnectionInfoDto
   */
  @Handler
  public void updateMasterIndex(Exchange exchange) throws JsonProcessingException {
    final String body = exchange.getIn().getBody(String.class);
    final ConnectionInfoDto connectionInfo = objectMapper.readValue(body, ConnectionInfoDto.class);

    var masterDoctorView = getMasterDoctorView(connectionInfo);

    doctorUpsertElasticSearchService.populateMasterIndex(masterDoctorView);
  }

  private MasterDoctorView getMasterDoctorView(ConnectionInfoDto connectionInfo) {
    return MasterDoctorView.builder()
        .tcsPersonId(connectionInfo.getTcsPersonId())
        .gmcReferenceNumber(connectionInfo.getGmcReferenceNumber())
        .doctorFirstName(connectionInfo.getDoctorFirstName())
        .doctorLastName(connectionInfo.getDoctorLastName())
        .submissionDate(connectionInfo.getSubmissionDate())
        .programmeName(connectionInfo.getProgrammeName())
        .membershipType(connectionInfo.getProgrammeMembershipType())
        .designatedBody(connectionInfo.getDesignatedBody())
        .tcsDesignatedBody(connectionInfo.getTcsDesignatedBody())
        .programmeOwner(connectionInfo.getProgrammeOwner())
        .connectionStatus(connectionInfo.getConnectionStatus())
        .membershipStartDate(connectionInfo.getProgrammeMembershipStartDate())
        .membershipEndDate(connectionInfo.getProgrammeMembershipEndDate())
        .build();
  }
}
