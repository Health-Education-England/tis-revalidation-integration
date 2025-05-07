package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.RevalidationIntegrationApplication;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class CdcDateDeserializerTest {

  private ObjectMapper mapper;
  private final static String CDC_DOC_JSON =
      """
          {
            "_id":{"_data":"01625a0706000001c001000001c000020042"},
            "operationType":"replace",
            "clusterTime":"Timestamp(1650067206, 448)",
            "ns":{"db":"revalidation","coll":"doctorsForDB"},
            "documentKey":{"_id":"1234567"},
            "fullDocument":{
                              "_id":"1234567","doctorFirstName":"First","doctorLastName":"Last",
                               "submissionDate":"2017-10-19 00:00:00","dateAdded":"2015-10-07 00:00:00",
                               "underNotice":"NO","sanction":"No","doctorStatus":"COMPLETED",
                               "lastUpdatedDate":"2022-04-15 00:00:00","designatedBodyCode":"1-AIIDWI",
                               "existsInGmc":false,"_class":"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"}
          }
          """;

  private final static String CDC_DOCDB_JSON_GMC_NUMBER =
      """
          {
            "gmcReferenceNumber":{"_data":"01625a0706000001c001000001c000020042"},
            "operationType":"replace",
            "clusterTime":"Timestamp(1650067206, 448)",
            "ns":{"db":"revalidation","coll":"doctorsForDB"},
            "documentKey":{"_id":"1234567"},
            "fullDocument":{
                              "_id":"1234567","doctorFirstName":"First","doctorLastName":"Last",
                              "submissionDate":"2017-10-19 00:00:00","dateAdded":"2015-10-07 00:00:00",
                              "underNotice":"NO","sanction":"No","doctorStatus":"COMPLETED",
                              "lastUpdatedDate":"2022-04-15 00:00:00","designatedBodyCode":"1-AIIDWI",
                              "existsInGmc":false,"_class":"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"}}
          """;


  private final static String CDC_DOCDB_EVENT_JSON =
      """
          {
            "_id": {"_data": "016819321a00000001010000000000020042"},
            "clusterTime": {"$timestamp": {"t": 1746481690, "i": 1}},
            "documentKey": {"_id": "1234567"},
            "fullDocument": {
                              "_id": "1234567", "doctorFirstName": "AAA", "doctorLastName": "BBB",
                              "submissionDate": {"$date": "2024-08-05T00:00:00Z"},
                              "dateAdded": {"$date": "2015-10-07T00:00:00Z"}, "underNotice": "YES",
                              "sanction": "No", "doctorStatus": "DRAFT",
                              "lastUpdatedDate": {"$date": "2025-04-29T00:00:00Z"},
                              "gmcLastUpdatedDateTime": {"$date": "2025-04-29T00:00:54.956Z"},
                              "designatedBodyCode": "1-1RSSQ05", "existsInGmc": true,
                              "_class": "uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"},
            "ns": {"db": "revalidation", "coll": "doctorsForDB"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [], "updatedFields": {"underNotice": "YES"}}
          }
          """;

  private final static String CDC_DOCDB_EVENT_JSON_DATE_INVALID =
      """
          {
            "_id": {"_data": "016819321a00000001010000000000020042"},
            "clusterTime": {"$timestamp": {"t": 1746481690, "i": 1}},
            "documentKey": {"_id": "1234567"},
            "fullDocument": {
                              "_id": "1234567", "doctorFirstName": "AAA", "doctorLastName": "BBB",
                              "submissionDate": {"$date": "05/08/2024"},
                              "dateAdded": {"$date": "07/10/2015"}, "underNotice": "YES",
                              "sanction": "No", "doctorStatus": "DRAFT",
                              "lastUpdatedDate": {"$date": "2025-04-29T00:00:00Z"},
                              "gmcLastUpdatedDateTime": {"$date": "2025-04-29T00:00:54.956Z"},
                              "designatedBodyCode": "1-1RSSQ05", "existsInGmc": true,
                              "_class": "uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"},
            "ns": {"db": "revalidation", "coll": "doctorsForDB"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [], "updatedFields": {"underNotice": "YES"}}
          }
          """;
  private String gmcId = "1234567";

  @BeforeEach
  void setup() {
    this.mapper = new RevalidationIntegrationApplication().mapper();
  }

  @ParameterizedTest
  @ValueSource(strings = {CDC_DOC_JSON, CDC_DOCDB_JSON_GMC_NUMBER})
  void shouldDeserializeGmcReferenceNumber(String jsonStr) throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(jsonStr,
            new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
            }
        );

    assertThat(document.getFullDocument().getGmcReferenceNumber(), is(gmcId));
  }

  @ParameterizedTest
  @ValueSource(strings = {CDC_DOC_JSON, CDC_DOCDB_EVENT_JSON})
  void shouldDeserializeDateStr(String jsonStr) throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(jsonStr, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
            }
        );

    LocalDate dateAdded = document.getFullDocument().getDateAdded();
    assertThat(dateAdded.getDayOfMonth(), is(7));
    assertThat(dateAdded.getMonthValue(), is(10));
    assertThat(dateAdded.getYear(), is(2015));
  }

  @Test
  void shouldThrowErrorWhenDateInvalid() {
    JsonMappingException thrown = assertThrows(JsonMappingException.class, () ->
        mapper.readValue(CDC_DOCDB_EVENT_JSON_DATE_INVALID, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
            }
        ), "Expected JsonMappingException to throw, but it didn't");

    assertTrue(thrown.getMessage().contains("Not supported date format:"));
  }
}
