package uk.nhs.hee.tis.revalidation.integration.cdc.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.util.CdcDateDeserializer;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcRecommendation {
  @Id
  private String id;
  private String gmcNumber;
  private RecommendationGmcOutcome outcome;
  private RecommendationType recommendationType;
  private RecommendationStatus recommendationStatus;
  @JsonDeserialize(using = CdcDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate gmcSubmissionDate;
  @JsonDeserialize(using = CdcDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate actualSubmissionDate;
  private String gmcRevalidationId;
  @JsonDeserialize(using = CdcDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate deferralDate;
  private String deferralReason;
  private String deferralSubReason;
  private List<String> comments;
  private String admin;
}
