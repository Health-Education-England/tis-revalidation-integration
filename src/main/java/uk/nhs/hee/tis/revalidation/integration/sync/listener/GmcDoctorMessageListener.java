package uk.nhs.hee.tis.revalidation.integration.sync.listener;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Slf4j
@Service
public class GmcDoctorMessageListener {
  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;


  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  public GmcDoctorMessageListener(DoctorUpsertElasticSearchService doctorUpsertElasticSearchService) {
    this.doctorUpsertElasticSearchService = doctorUpsertElasticSearchService;
  }

  @SqsListener(value = "${cloud.aws.end-point.uri}")
  public void getMessage(DoctorsForDB doctor) {
    log.info("Message from Received from AWS SQS Queue - " + doctor.getGmcReferenceNumber());
    log.info("Message from Received from AWS SQS Queue - " + doctor.getSubmissionDate());

    //prepare the MasterDoctorView and call the service method

   MasterDoctorView masterDoctorView =  MasterDoctorView.builder()
       .tcsPersonId(null)
        .gmcReferenceNumber(doctor.getGmcReferenceNumber())
        .doctorFirstName(doctor.getDoctorFirstName())
        .doctorLastName(doctor.getDoctorLastName())
        .submissionDate(doctor.getSubmissionDate())
       .programmeName("No Programme Name")
       .programmeOwner("No Programme Owner")
        .designatedBody(doctor.getDesignatedBodyCode())
        .build();
        //.connectionStatus(doctor.)

    doctorUpsertElasticSearchService.populateMasterIndex(masterDoctorView);



  }

  /*public void saveExceptionViews(ExceptionView dataToSave) {
    // find trainee record from Exception ES index
    Iterable<ExceptionView> existingRecords = findExceptionViewsByGmcNumberPersonId(dataToSave);

    // if trainee already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateExceptionViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addExceptionViews(dataToSave);
    }
*/
  /*@SqsListener(value = "${orders.queue.name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void processMessage(String message) {
    try {
      log.debug("Received new SQS message: {}", message );
      OrderDto orderDto = OBJECT_MAPPER.readValue(message, OrderDto.class);

      this.orderService.processOrder(orderDto);

    } catch (Exception e) {
      throw new RuntimeException("Cannot process message from SQS", e);
    }
  }*/

}
