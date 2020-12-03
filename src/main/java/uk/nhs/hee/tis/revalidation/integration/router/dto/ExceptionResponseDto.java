package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExceptionResponseDto {

  private long countTotal;
  private long totalPages;
  private long totalResults;
  private List<ExceptionRecordDto> exceptionRecord;
}
