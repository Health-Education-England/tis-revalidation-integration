package uk.nhs.hee.tis.revalidation.integration.router.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectionTcsDto {

  private long totalResults;
  private long totalPages;
  private List<ConnectionTcsRecordDto> connections;
}
