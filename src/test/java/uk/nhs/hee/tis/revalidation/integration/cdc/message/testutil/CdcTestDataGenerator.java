/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil;

import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.ADMIN;
import static uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus.DRAFT;
import static uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus.SUBMITTED_TO_GMC;
import static uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice.YES;

import com.mongodb.client.model.changestream.OperationType;
import java.time.Instant;
import java.time.LocalDate;
import org.bson.BsonDateTime;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationType;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Component
public class CdcTestDataGenerator {

  public static final String GMC_REFERENCE_NUMBER_VAL = "111";
  public static final String DOCTOR_FIRST_NAME_VAL = "firstName";
  public static final String DOCTOR_LAST_NAME_VAL = "lastName";
  public static final BsonDateTime SUBMISSION_DATE_VAL =
      new BsonDateTime(Instant.now().getEpochSecond());
  public static final BsonDateTime DATE_ADDED_VAL =
      new BsonDateTime(Instant.now().getEpochSecond());
  public static final UnderNotice UNDER_NOTICE_VAL = YES;
  public static final String SANCTION_VAL = "sanction";
  public static final RecommendationStatus DOCTOR_STATUS_VAL = SUBMITTED_TO_GMC;
  public static final BsonDateTime LAST_UPDATED_DATE_VAL =
      new BsonDateTime(Instant.now().getEpochSecond());
  public static final String DESIGNATED_BODY_CODE_VAL = "designatedBodyCode";
  public static final String ADMIN_VAL = "admin";
  public static final Boolean EXISTS_IN_GMC_VAL = true;
  public static final String PROGRAMME_NAME_VAL = "Dev 4 pain";
  public static final String PROGRAMME_TYPE_VAL = "prog-type";
  public static final String PROGRAMME_OWNER_VAL = "squad";
  public static final String CONNECTION_STATUS_VAL = "inappropriate";
  public static final String DATA_SOURCE_VAL = "Lundan";
  public static final String C_I = "ci";

  private static DoctorsForDB doctorsForDB = DoctorsForDB.builder()
      .gmcReferenceNumber(GMC_REFERENCE_NUMBER_VAL)
      .doctorFirstName(DOCTOR_FIRST_NAME_VAL)
      .doctorFirstName(DOCTOR_LAST_NAME_VAL)
      .submissionDate(LocalDate.now())
      .dateAdded(LocalDate.now())
      .underNotice(UNDER_NOTICE_VAL)
      .sanction(SANCTION_VAL)
      .doctorStatus(DOCTOR_STATUS_VAL)
      .lastUpdatedDate(LocalDate.now())
      .designatedBodyCode(DESIGNATED_BODY_CODE_VAL)
      .admin(ADMIN_VAL)
      .existsInGmc(EXISTS_IN_GMC_VAL)
      .build();

  private static Recommendation recommendation = Recommendation.builder()
      .id("1")
      .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
      .recommendationType(RecommendationType.REVALIDATE)
      .recommendationStatus(DRAFT)
      .gmcSubmissionDate(LocalDate.now().plusMonths(6))
      .admin(ADMIN_VAL)
      .build();

  /**
   * Get a test instance of MasterDoctorView.
   *
   * @return MasterDoctorView test instance
   */
  public static MasterDoctorView getTestMasterDoctorView() {
    return MasterDoctorView.builder()
        .id("1")
        .tcsPersonId(1L)
        .gmcReferenceNumber(GMC_REFERENCE_NUMBER_VAL)
        .doctorFirstName("old" + DOCTOR_FIRST_NAME_VAL)
        .doctorLastName("old" + DOCTOR_LAST_NAME_VAL)
        .submissionDate(LocalDate.now())
        .designatedBody("old" + DESIGNATED_BODY_CODE_VAL)
        .tisStatus(DRAFT)
        .lastUpdatedDate(LocalDate.now())
        .admin("old" + ADMIN)
        .existsInGmc(false)
        .build();
  }

  /**
   * Get a test instance of an insert DoctorsForDb CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcDoctor test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorInsertCdcDocumentDto() {
    return new CdcDocumentDto<DoctorsForDB>(OperationType.INSERT.getValue(), doctorsForDB);
  }

  /**
   * Get a test instance of an replace DoctorsForDb CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcDoctor test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorReplaceCdcDocumentDto() {
    return new CdcDocumentDto<DoctorsForDB>(OperationType.REPLACE.getValue(), doctorsForDB);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationInsertCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder()
        .id("1")
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .outcome(RecommendationGmcOutcome.APPROVED)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.INSERT.getValue(), recommendation);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto with a null outcome.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation>
      getCdcRecommendationInsertCdcDocumentDtoNullOutcome() {
    Recommendation recommendation = Recommendation.builder()
        .id("1")
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.INSERT.getValue(), recommendation);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationReplaceCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder()
        .id("1")
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.REPLACE.getValue(), recommendation);
  }

  /**
   * Get a test instance of an unsupported doctor change operation.
   *
   * @return CdcDocumentDto CdcDoctor unsupported test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorUnsupportedCdcDocumentDto() {
    DoctorsForDB doctorsForDB = DoctorsForDB.builder().build();

    return new CdcDocumentDto<DoctorsForDB>(OperationType.DROP.getValue(), doctorsForDB);
  }

  /**
   * Get a test instance of an unsupported recommendation change operation.
   *
   * @return CdcDocumentDto CdcRecommendation unsupported test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationUnsupportedCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder().build();

    return new CdcDocumentDto<Recommendation>(OperationType.DROP.getValue(), recommendation);
  }

  /**
   * Create a new ConnectionInfoDto based on a trainee on a programme with a "period of grace".
   *
   * @return A {@link ConnectionInfoDto} for a trainee on a current Programme
   */
  public static ConnectionInfoDto getConnectionInfo() {
    return ConnectionInfoDto.builder().tcsPersonId(1984L)
        .designatedBody(C_I + DESIGNATED_BODY_CODE_VAL)
        .tcsDesignatedBody(C_I + "tcs" + DESIGNATED_BODY_CODE_VAL)
        .doctorFirstName(C_I + DOCTOR_FIRST_NAME_VAL).doctorLastName(C_I + DOCTOR_LAST_NAME_VAL)
        .gmcReferenceNumber(C_I + GMC_REFERENCE_NUMBER_VAL)
        .programmeName(C_I + PROGRAMME_NAME_VAL).programmeOwner(C_I + PROGRAMME_OWNER_VAL)
        .programmeMembershipType(C_I + PROGRAMME_TYPE_VAL)
        .programmeMembershipStartDate(LocalDate.now().minusMonths(2))
        .programmeMembershipEndDate(LocalDate.now().plusYears(3L).plusMonths(3))
        .curriculumEndDate(LocalDate.now().plusYears(3L))
        .tisConnectionStatus(C_I + CONNECTION_STATUS_VAL).dataSource(C_I + DATA_SOURCE_VAL)
        .build();
  }
}
