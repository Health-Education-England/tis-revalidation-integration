package uk.nhs.hee.tis.revalidation.integration.router.dto;

import java.util.List;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;

/**
 * A DTO class for displaying a list of details of hidden discrepancies.
 */
public record HiddenDiscrepancyInfoDto(
    String gmcReferenceNumber,
    String doctorFirstName,
    String doctorLastName,
    String programmeName,
    String designatedBody,
    String tcsDesignatedBody,
    List<HiddenDiscrepancy> hiddenDiscrepancies
) {}
