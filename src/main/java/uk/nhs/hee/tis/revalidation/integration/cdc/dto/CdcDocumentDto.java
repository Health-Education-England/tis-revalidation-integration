package uk.nhs.hee.tis.revalidation.integration.cdc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.changestream.UpdateDescription;
import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcDocumentDto<T> {
  private String operationType;
  private T fullDocument;
  @Nullable
  private UpdateDescription updateDescription;
}
