package uk.nhs.hee.tis.revalidation.integration.router.dto;

import java.util.List;

/**
 * A DTO class for displaying a list of details of hidden discrepancies and pagination info.
 */
public record HiddenDiscrepancySummaryDto (
     long countTotal,
     long totalPages,
     long totalResults,
     List<HiddenDiscrepancyInfoDto> hiddenDiscrepancies
) {}
