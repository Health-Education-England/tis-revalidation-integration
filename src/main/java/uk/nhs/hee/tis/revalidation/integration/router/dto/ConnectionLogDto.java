package uk.nhs.hee.tis.revalidation.integration.router.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionRequestType;

/**
 * A data class to handle connection log details.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectionLogDto {
  private String gmcId;
  private String newDesignatedBodyCode;
  private String previousDesignatedBodyCode;
  private String updatedBy;
  private LocalDateTime eventDateTime;
  private String gmcClientId;
  private String reason;
  private ConnectionRequestType requestType;
  private String responseCode;
}
