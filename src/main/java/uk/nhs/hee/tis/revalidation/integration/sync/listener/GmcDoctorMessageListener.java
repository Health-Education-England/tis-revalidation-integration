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
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class GmcDoctorMessageListener {

  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;


  private final DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  public GmcDoctorMessageListener(
      DoctorUpsertElasticSearchService doctorUpsertElasticSearchService) {
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
  }

  @SqsListener(value = "${cloud.aws.end-point.uri}")
  public void getMessage(DoctorsForDB doctor) {
    log.info("Message received from AWS SQS Queue - {}" , doctor.getGmcReferenceNumber());

    //prepare the MasterDoctorView and call the service method
    MasterDoctorView masterDoctorView = MasterDoctorView.builder()
        .tcsPersonId(null)
        .gmcReferenceNumber(doctor.getGmcReferenceNumber())
        .doctorFirstName(doctor.getDoctorFirstName())
        .doctorLastName(doctor.getDoctorLastName())
        .submissionDate(doctor.getSubmissionDate())
        .programmeName("No Programme Name")
        .membershipType("No Membership Type")
        .designatedBody(doctor.getDesignatedBodyCode())
        .tcsDesignatedBody("No TCS DBC")
        .programmeOwner("No Programme Owner")
        .connectionStatus(getConnectionStatus(doctor))
        //.membershipStartDate(null)
        //.membershipEndDate(null)
        .build();

    doctorUpsertElasticSearchService.populateMasterIndex(masterDoctorView);

  }

  private String getConnectionStatus(DoctorsForDB doctorsForDB) {
    return (doctorsForDB.getDesignatedBodyCode() != null) ? "Yes" : "No";
  }
}
