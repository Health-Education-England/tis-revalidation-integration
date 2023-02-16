/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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
import uk.nhs.hee.tis.revalidation.integration.router.dto.ConnectionTcsDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.integration.router.dto.TraineeSummaryDto;

@Slf4j
@Component
public class ConnectionExceptionAggregationStrategy implements AggregationStrategy {

  @Autowired
  private ObjectMapper mapper;

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    final var result = new DefaultExchange(new DefaultCamelContext());

    final var messageBody = oldExchange.getIn().getBody();
    final var traineeSummaryDto = mapper.convertValue(messageBody, TraineeSummaryDto.class);
    final var traineeInfos = traineeSummaryDto.getTraineeInfo();

    if (traineeInfos != null) {
      final var connectionExceptionDto = aggregateTraineeWithConnection(traineeInfos, newExchange);
      result.getMessage().setBody(connectionExceptionDto);
    } else {
      final var connectionExceptionDto = aggregateTraineeWithConnectionAllowNull(newExchange);
      log.info("The exception connections from tcs when reval does not have exception connection :{}", connectionExceptionDto);
      result.getMessage().setBody(connectionExceptionDto);
    }
    return result;
  }

  private ConnectionTcsDto aggregateTraineeWithConnection(List<TraineeInfoDto> traineeInfos,
      Exchange newExchange) {
    final var body = newExchange.getIn().getBody();
    final var connectionExceptionDto = mapper.convertValue(body, ConnectionTcsDto.class);
    final var connections = connectionExceptionDto.getConnections();

    final var connectionExceptionRecordDtos = connections.stream().map(conn -> {
      final var traineeInfoDto = traineeInfos.stream()
          .filter(t -> t.getGmcReferenceNumber().equals(conn.getGmcReferenceNumber())).findFirst();

      conn.setTcsDesignatedBody(conn.getDesignatedBody());
      conn.setDesignatedBody(null);
      conn.setTisConnectionStatus(getConnectionStatus(null));
      if (traineeInfoDto.isPresent()) {
        final var trainee = traineeInfoDto.get();
        conn.setSubmissionDate(trainee.getSubmissionDate());
        conn.setDesignatedBody(trainee.getDesignatedBody());
        conn.setTisConnectionStatus(getConnectionStatus(trainee.getDesignatedBody()));
      }
      return conn;
    }).collect(toList());

    connectionExceptionDto.setConnections(connectionExceptionRecordDtos);
    return connectionExceptionDto;
  }

  private ConnectionTcsDto aggregateTraineeWithConnectionAllowNull(Exchange newExchange) {
    final var body = newExchange.getIn().getBody();
    final var connectionExceptionDto = mapper.convertValue(body, ConnectionTcsDto.class);
    final var connections = connectionExceptionDto.getConnections();
    final var connectionExceptionRecordDtos = connections.stream().map(conn -> {
      // Designated Body and connectionStatus should base on data from  GMC
      // where it does not exist here
      conn.setTcsDesignatedBody(conn.getDesignatedBody());
      conn.setDesignatedBody(null);
      conn.setTisConnectionStatus(getConnectionStatus(null));
      return conn;
    }).collect(toList());

    connectionExceptionDto.setConnections(connectionExceptionRecordDtos);
    return connectionExceptionDto;

  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }

}
