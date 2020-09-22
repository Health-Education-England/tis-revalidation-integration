package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraineeRecommendationDto {

  private String fullName;
  private String gmcNumber;
  private String programmeMembershipType;
  private String currentGrade;
  private LocalDate cctDate;
  private String underNotice;
  private List<TraineeRecommendationRecordDto> revalidations;
  private List<DeferralReasonDto> deferralReasons;

}
