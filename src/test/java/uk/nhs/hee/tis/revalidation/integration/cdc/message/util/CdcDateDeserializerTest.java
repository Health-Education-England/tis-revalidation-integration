package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.RevalidationIntegrationApplication;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;

@ExtendWith(MockitoExtension.class)
class CdcDateDeserializerTest {

  private ObjectMapper mapper;
  private static final String CDC_DOC_JSON =
      """
          {
            "_id":{"_data":"01625a0706000001c001000001c000020042"},
            "operationType":"replace",
            "clusterTime":"Timestamp(1650067206, 448)",
            "ns":{"db":"revalidation","coll":"doctorsForDB"},
            "documentKey":{"_id":"1234567"},
            "fullDocument":{
                              "_id":"1234567","doctorFirstName":"First",
                              "doctorLastName":"Last",
                              "submissionDate":"2017-10-19 00:00:00",
                              "dateAdded":"2015-10-07 00:00:00",
                              "underNotice":"NO","sanction":"No",
                              "doctorStatus":"COMPLETED",
                              "lastUpdatedDate":"2022-04-15 00:00:00",
                              "designatedBodyCode":"1-AIIDWI",
                              "existsInGmc":false,
                              "_class":"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"}
          }
          """;

  private static final String CDC_DOCDB_EVENT_JSON =
      """
          {
            "_id": {"_data": "016819321a00000001010000000000020042"},
            "clusterTime": {"$timestamp": {"t": 1746481690, "i": 1}},
            "documentKey": {"_id": "1234567"},
            "fullDocument": {
                              "_id": "1234567", "doctorFirstName": "AAA", "doctorLastName": "BBB",
                              "submissionDate": {"$date": "2024-08-05T00:00:00Z"},
                              "dateAdded": {"$date": "2015-10-07T00:00:00Z"}, "underNotice": "NO",
                              "sanction": "No", "doctorStatus": "DRAFT",
                              "lastUpdatedDate": {"$date": "2025-04-29T00:00:00Z"},
                              "gmcLastUpdatedDateTime": {"$date": "2025-04-29T00:00:54.956Z"},
                              "designatedBodyCode": "1-1RSSQ05", "existsInGmc": false,
                              "_class": "uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"},
            "ns": {"db": "revalidation", "coll": "doctorsForDB"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [],
                                  "updatedFields": {"underNotice": "YES"}}
          }
          """;

  private static final String CDC_RECOMMENDATION_EVENT_JSON =
      """
          {
            "_id": {"_data": "0168220a440000000b01000000000002d1b5"},
            "clusterTime": {"$timestamp": {"t": 1747061316, "i": 11}},
            "documentKey": {"_id": {"$oid": "67fcf9ea74f4e44093b9f327"}},
            "fullDocument": {
                              "_id": {"$oid": "67fcf9ea74f4e44093b9f327"},
                              "gmcNumber": "1234567", "recommendationType": "REVALIDATE",
                              "recommendationStatus": "SUBMITTED_TO_GMC",
                              "gmcSubmissionDate": {"$date": "2025-04-28T00:00:00Z"},
                              "comments": ["test"], "admin": "aaa.bbb@ccc.com",
                              "_class": "uk.nhs.hee.tis.revalidation.entity.Recommendation"},
            "ns": {"db": "revalidation", "coll": "recommendation"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [],
                                  "updatedFields": {"recommendationStatus": "SUBMITTED_TO_GMC"}}}
          """;

  @BeforeEach
  void setup() {
    this.mapper = new RevalidationIntegrationApplication().mapper();
  }

  @ParameterizedTest
  @ValueSource(strings = {CDC_DOC_JSON, CDC_DOCDB_EVENT_JSON})
  void shouldDeserializeDocDbJsonStr(String jsonStr) throws JsonProcessingException {
    CdcDocumentDto<DoctorsForDB> document =
        mapper.readValue(jsonStr, new TypeReference<CdcDocumentDto<DoctorsForDB>>() {
            }
        );

    DoctorsForDB doctorsForDb = document.getFullDocument();
    assertThat(doctorsForDb.getUnderNotice(), is(UnderNotice.NO));
    assertThat(doctorsForDb.getExistsInGmc(), is(false));

    LocalDate dateAdded = doctorsForDb.getDateAdded();
    assertThat(dateAdded.getDayOfMonth(), is(7));
    assertThat(dateAdded.getMonthValue(), is(10));
    assertThat(dateAdded.getYear(), is(2015));
  }

  @Test
  void shouldDeserializeRecommendationJsonStr() throws JsonProcessingException {
    CdcDocumentDto<Recommendation> document =
        mapper.readValue(CDC_RECOMMENDATION_EVENT_JSON,
            new TypeReference<CdcDocumentDto<Recommendation>>() {
            }
        );

    Recommendation recommendation = document.getFullDocument();
    assertThat(recommendation.getGmcNumber(), is("1234567"));
    assertThat(recommendation.getRecommendationStatus(), is(RecommendationStatus.SUBMITTED_TO_GMC));

    LocalDate dateAdded = recommendation.getGmcSubmissionDate();
    assertThat(dateAdded.getDayOfMonth(), is(28));
    assertThat(dateAdded.getMonthValue(), is(4));
    assertThat(dateAdded.getYear(), is(2025));
  }
}
