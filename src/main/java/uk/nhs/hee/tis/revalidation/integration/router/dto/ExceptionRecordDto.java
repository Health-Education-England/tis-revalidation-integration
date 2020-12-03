package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExceptionRecordDto {

  private String gmcId;
  private String exceptionMessage;
}
