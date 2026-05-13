package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_DOCDB_EVENT_JSON;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_DOC_JSON;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_RECOMMENDATION_EVENT_JSON;

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
