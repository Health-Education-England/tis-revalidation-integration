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
                "_id": { "_data": "0168f0e0910000000a0100000000000210b5" },
                "clusterTime": { "$timestamp": { "t": 1760616593, "i": 10 } },
                "documentKey": { "_id": "b401507d-9285-4136-b5dc-c85e25253db0" },
                "fullDocument": {
                  "_id": "b401507d-9285-4136-b5dc-c85e25253db0",
                  "gmcId": "7999999",
                  "gmcClientId": "HEE161020251309837B93A2",
                  "newDesignatedBodyCode": "1-1RSSQ05",
                  "reason": "1",
                  "requestType": "ADD",
                  "requestTime": { \s
                      "$date": "2025-10-10T00:00:00.000Z"\s
                  },\s
                  "responseCode": "0",
                  "updatedBy": "Emily",
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
    assertThat(connectionLog.getGmcId(), is("7999999"));
    assertThat(connectionLog.getUpdatedBy(), is("Emily"));

    LocalDateTime dateAdded = connectionLog.getRequestTime();
    assertThat(dateAdded.getDayOfMonth(), is(10));
    assertThat(dateAdded.getMonthValue(), is(10));
    assertThat(dateAdded.getYear(), is(2025));
  }
}
