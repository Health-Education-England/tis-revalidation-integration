package uk.nhs.hee.tis.revalidation.integration.router.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Component
public class TraineeDetailProcessor implements Processor {

  @Autowired
  ObjectMapper mapper;

  @Override
  public void process(Exchange exchange) throws Exception {
    String body = exchange.getIn().getBody(String.class);
    Map<String, Object> map = mapper.readValue(body, Map.class);
    TraineeSummaryDto traineeSummaryDto = mapper.convertValue(map.get("doctor"),
        TraineeSummaryDto.class);
    TraineeDetailsDto traineeDetailsDto = mapper.convertValue(map.get("programme"),
        TraineeDetailsDto.class);
    TraineeNotesDto traineeNotesDto = mapper.convertValue(map.get("notes"), TraineeNotesDto.class);

    TraineeDetailsDto traineeDetailsDtoResult = null;
    if (traineeSummaryDto != null & traineeSummaryDto.getCountTotal() == 1) {
      if (traineeDetailsDto != null & traineeDetailsDto.getGmcNumber() != null) {
        traineeDetailsDtoResult = traineeDetailsDto;
      } else {
        TraineeInfoDto traineeInfoDto = traineeSummaryDto.getTraineeInfo().get(0);
        traineeDetailsDtoResult = TraineeDetailsDto.builder()
            .gmcNumber(traineeInfoDto.getGmcReferenceNumber())
            .forenames(traineeInfoDto.getDoctorFirstName())
            .surname(traineeInfoDto.getDoctorLastName()).build();
      }
      traineeDetailsDtoResult.setNotes(traineeNotesDto.getNotes());
    }

    exchange.getIn().setBody(traineeDetailsDtoResult);
  }
}
