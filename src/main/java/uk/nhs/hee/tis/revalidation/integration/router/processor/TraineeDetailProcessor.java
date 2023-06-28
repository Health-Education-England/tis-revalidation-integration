/*
 * The MIT License (MIT)
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.router.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeNotesDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Component
public class TraineeDetailProcessor implements Processor {

  protected static final String TRAINEE_AGGREGATION_HEADER = "programme";
  protected static final String GMC_AGGREGATION_HEADER = "doctor";
  protected static final String NOTES_AGGREGATION_HEADER = "notes";

  ObjectMapper mapper;

  TraineeDetailProcessor(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String body = exchange.getIn().getBody(String.class);
    Map<String, Object> map = mapper.readValue(body, Map.class);
    TraineeDetailsDto traineeDetailsDto = mapper.convertValue(map.get(TRAINEE_AGGREGATION_HEADER),
        TraineeDetailsDto.class);
    TraineeSummaryDto traineeSummaryDto = mapper.convertValue(map.get(GMC_AGGREGATION_HEADER),
        TraineeSummaryDto.class);
    TraineeNotesDto traineeNotesDto = mapper.convertValue(map.get(NOTES_AGGREGATION_HEADER),
        TraineeNotesDto.class);

    TraineeDetailsDto traineeDetailsDtoResult = null;
    if (traineeDetailsDto != null & traineeDetailsDto.getGmcNumber() != null) {
      traineeDetailsDtoResult = traineeDetailsDto;
    }

    if (traineeSummaryDto != null & traineeSummaryDto.getCountTotal() == 1) {
      if (traineeDetailsDtoResult == null) {
        traineeDetailsDtoResult = TraineeDetailsDto.builder().build();
      }
      TraineeInfoDto traineeInfoDto = traineeSummaryDto.getTraineeInfo().get(0);
      traineeDetailsDtoResult.setGmcNumber(traineeInfoDto.getGmcReferenceNumber());
      traineeDetailsDtoResult.setSurname(traineeInfoDto.getDoctorLastName());
      traineeDetailsDtoResult.setForenames(traineeInfoDto.getDoctorFirstName());
    }

    if (traineeDetailsDtoResult != null) {
      traineeDetailsDtoResult.setNotes(traineeNotesDto.getNotes());
      exchange.getIn().setBody(traineeDetailsDtoResult);
    } else {
      exchange.getIn().setBody(null);
      exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.NOT_FOUND.value());
    }
  }
}
