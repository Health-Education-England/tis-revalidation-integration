package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraineeRecommendationRecordDto {

  private String gmcNumber;
  private String recommendationId;
  private String gmcOutcome;
  private String recommendationType;
  private LocalDate gmcSubmissionDate;
  private LocalDate actualSubmissionDate;
  private String gmcRevalidationId;
  private String recommendationStatus;
  private LocalDate deferralDate;
  private String deferralReason;
  private String deferralSubReason;
  private String deferralComment;
  private List<String> comments;
  private String admin;
}
