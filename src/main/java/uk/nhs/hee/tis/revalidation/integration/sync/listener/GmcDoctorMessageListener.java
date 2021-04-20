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
    log.info("Message from Received from AWS SQS Queue - " + doctor.getGmcReferenceNumber());
    log.info("Message from Received from AWS SQS Queue - " + doctor.getSubmissionDate());

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
        .membershipStartDate(null)
        .membershipEndDate(null)
        .build();

    doctorUpsertElasticSearchService.populateMasterIndex(masterDoctorView);

  }

  private String getConnectionStatus(DoctorsForDB doctorsForDB) {
    return (doctorsForDB.getDesignatedBodyCode() != null) ? "Yes" : "No";
  }
}
