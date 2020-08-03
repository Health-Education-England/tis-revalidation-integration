package uk.nhs.hee.tis.revalidation.integration.router.processor;

import static java.util.stream.Collectors.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class GmcIdProcessorBean {

  @Autowired
  private ObjectMapper mapper;

  public List<String> process(Exchange exchange) throws JsonProcessingException {
    final var body = exchange.getIn().getBody();
    final var traineeSummaryDto = mapper.convertValue(body, TraineeSummaryDto.class);
    return traineeSummaryDto.getTraineeInfo().stream()
        .map(t -> t.getGmcReferenceNumber()).collect(toList());
  }
}
