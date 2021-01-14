package uk.nhs.hee.tis.revalidation.integration.router.aggregation;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionHiddenDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class ConnectionHiddenAggregationStrategy implements AggregationStrategy {

  @Autowired
  private ObjectMapper mapper;

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final var messageBody = oldExchange.getIn().getBody();
    final var traineeSummaryDto = mapper.convertValue(messageBody, TraineeSummaryDto.class);
    final var traineeInfos = traineeSummaryDto.getTraineeInfo();

    if (traineeInfos != null) {
      final var connectionHiddenDto = aggregateTraineeWithConnection(traineeInfos, newExchange);
      result.getMessage().setBody(connectionHiddenDto);
    } else {
      final var connectionHiddenDto = aggregateTraineeWithConnectionAllowNull(newExchange);
      log.info("The hidden connections from tcs when reval does not have hidden connection :{}", connectionHiddenDto);
      result.getMessage().setBody(connectionHiddenDto);
    }
    return result;
  }

  private ConnectionHiddenDto aggregateTraineeWithConnection(List<TraineeInfoDto> traineeInfos,
      Exchange newExchange) {
    final var body = newExchange.getIn().getBody();
    final var connectionHiddenDto = mapper.convertValue(body, ConnectionHiddenDto.class);
    final var connections = connectionHiddenDto.getConnections();

    final var connectionHiddenRecordDtos = connections.stream().map(conn -> {
      final var traineeInfoDto = traineeInfos.stream()
          .filter(t -> t.getGmcReferenceNumber().equals(conn.getGmcReferenceNumber())).findFirst();
      if (traineeInfoDto.isPresent()) {
        final var trainee = traineeInfoDto.get();
        conn.setSubmissionDate(trainee.getSubmissionDate());
        conn.setDesignatedBody(trainee.getDesignatedBody());
      }

      conn.setConnectionStatus(getConnectionStatus(conn.getDesignatedBody()));
      return conn;
    }).collect(toList());

    connectionHiddenDto.setConnections(connectionHiddenRecordDtos);
    return connectionHiddenDto;
  }

  private ConnectionHiddenDto aggregateTraineeWithConnectionAllowNull(Exchange newExchange) {
    final var body = newExchange.getIn().getBody();
    final var connectionHiddenDto = mapper.convertValue(body, ConnectionHiddenDto.class);
    final var connections = connectionHiddenDto.getConnections();
    final var connectionHiddenRecordDtos = connections.stream().map(conn -> {
      conn.setConnectionStatus(getConnectionStatus(conn.getDesignatedBody()));
      return conn;
    }).collect(toList());

    connectionHiddenDto.setConnections(connectionHiddenRecordDtos);
    return connectionHiddenDto;

  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
