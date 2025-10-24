package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.RevalidationIntegrationApplication;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;

@ExtendWith(MockitoExtension.class)
class CdcLocalDateTimeDeserializerTest {

  private static final String CDC_CONNECTION_EVENT_JSON =
      """
              {
                "_id": { "_data": "0999f0e0990000000a0900000000000999b9" },
                "clusterTime": { "$timestamp": { "t": 176069999, "i": 10 } },
                "documentKey": { "_id": "b99999d-9999-9999-b9dc-c99e9999db9" },
                "fullDocument": {
                  "_id": "b999999d-9999-9999-b9dc-c99e99999db0",
                  "gmcId": "9999999",
                  "gmcClientId": "ABC999999999999999A9",
                  "newDesignatedBodyCode": "1-1AB999",
                  "reason": "1",
                  "requestType": "ADD",
                  "requestTime": { \s
                      "$date": "2025-10-10T00:00:00.000Z"\s
                  },\s
                  "responseCode": "0",
                  "updatedBy": "Test",
                  "_class": "uk.nhs.hee.tis.revalidation.connection.entity.ConnectionRequestLog"
                },
                "ns": { "db": "revalidation", "coll": "connectionLogs" },
                "operationType": "insert"
              }
          """;
  private ObjectMapper mapper;

  @BeforeEach
  void setup() {
    this.mapper = new RevalidationIntegrationApplication().mapper();
  }

  @Test
  void shouldDeserializeConnectionJsonStr() throws JsonProcessingException {
    CdcDocumentDto<ConnectionLog> document =
        mapper.readValue(CDC_CONNECTION_EVENT_JSON,
            new TypeReference<CdcDocumentDto<ConnectionLog>>() {
            }
        );

    ConnectionLog connectionLog = document.getFullDocument();
    assertThat(connectionLog.getGmcId(), is("9999999"));
    assertThat(connectionLog.getUpdatedBy(), is("Test"));

    LocalDateTime dateAdded = connectionLog.getRequestTime();
    assertThat(dateAdded.getDayOfMonth(), is(10));
    assertThat(dateAdded.getMonthValue(), is(10));
    assertThat(dateAdded.getYear(), is(2025));
  }
}
