package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;

@ExtendWith(MockitoExtension.class)
public class CdcDateDeserializerTest {

  private ObjectMapper mapper = new ObjectMapper();
  private String cdcDocumentJson = "{\"_id\":{\"_data\":\"01625a0706000001c001000001c000020042\"},"
      + "\"operationType\":\"replace\",\"clusterTime\":\"Timestamp(1650067206, 448)\",\"ns\":{\"db\":"
      + "\"revalidation\",\"coll\":\"doctorsForDB\"},\"documentKey\":{\"_id\":\"7072196\"},"
      + "\"fullDocument\":{\"_id\":\"1234567\",\"doctorFirstName\":\"Kate\",\"doctorLastName\":"
      + "\"Sherring\",\"submissionDate\":\"2017-10-19 00:00:00\",\"dateAdded\":"
      + "\"2015-10-07 00:00:00\",\"underNotice\":\"NO\",\"sanction\":\"No\",\"doctorStatus\":"
      + "\"COMPLETED\",\"lastUpdatedDate\":\"2022-04-15 00:00:00\",\"designatedBodyCode\":"
      + "\"1-AIIDWI\",\"existsInGmc\":false,\"_class\":"
      + "\"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB\"}}";
  private String cdcDocumentJsonGmcReferencNumber =
      "{\"gmcReferenceNumber\":{\"_data\":\"01625a0706000001c001000001c000020042\"},"
          + "\"operationType\":\"replace\",\"clusterTime\":\"Timestamp(1650067206, 448)\",\"ns\":{\"db\":"
          + "\"revalidation\",\"coll\":\"doctorsForDB\"},\"documentKey\":{\"_id\":\"7072196\"},"
          + "\"fullDocument\":{\"_id\":\"1234567\",\"doctorFirstName\":\"Kate\",\"doctorLastName\":"
          + "\"Sherring\",\"submissionDate\":\"2017-10-19 00:00:00\",\"dateAdded\":"
          + "\"2015-10-07 00:00:00\",\"underNotice\":\"NO\",\"sanction\":\"No\",\"doctorStatus\":"
          + "\"COMPLETED\",\"lastUpdatedDate\":\"2022-04-15 00:00:00\",\"designatedBodyCode\":"
          + "\"1-AIIDWI\",\"existsInGmc\":false,\"_class\":"
          + "\"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB\"}}";
  private String gmcId = "1234567";

  @Test
  void shouldDeserializeGmcReferenceNumber() throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(cdcDocumentJson, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
        });

    assertThat(document.getFullDocument().getGmcReferenceNumber(), is(gmcId));
  }

  @Test
  void shouldDeserializeGmcReferenceNumberFromAlias() throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(cdcDocumentJsonGmcReferencNumber,
            new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
        });

    assertThat(document.getFullDocument().getGmcReferenceNumber(), is(gmcId));
  }

  @Test
  void shouldDeserializeMongoDateString() throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(cdcDocumentJson, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
        });

    assertThat(document.getFullDocument().getDateAdded().getDayOfMonth(), is(7));
    assertThat(document.getFullDocument().getDateAdded().getMonthValue(), is(10));
    assertThat(document.getFullDocument().getDateAdded().getYear(), is(2015));
  }

  @Test
  void shouldDeserializeLocalDateString() throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(cdcDocumentJson, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
        });
    var newJson = mapper.writeValueAsString(document.getFullDocument());
    var doctorsForDb = mapper.readValue(newJson, DoctorsForDB.class);

    assertThat(doctorsForDb.getDateAdded().getDayOfMonth(), is(7));
    assertThat(doctorsForDb.getDateAdded().getMonthValue(), is(10));
    assertThat(doctorsForDb.getDateAdded().getYear(), is(2015));
  }

}
