package uk.nhs.hee.tis.revalidation.integration.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionInfoDto {

  String gmcReferenceNumber;
  String doctorFirstName;
  String doctorLastName;
  LocalDate submissionDate;
  String programmeName;
  String programmeMembershipType;
  String designatedBody;
  String programmeOwner;
  String connectionStatus;
  LocalDate programmeMembershipStartDate;
  LocalDate programmeMembershipEndDate;
}
