package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import uk.nhs.hee.tis.revalidation.integration.enums.Status;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeferralReasonDto {

  private String code;
  private String reason;
  private String abbr;
  private List<DeferralReasonDto> subReasons;
  private Status status;
}
