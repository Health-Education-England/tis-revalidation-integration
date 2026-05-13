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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentKey;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcHiddenDiscrepancyDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.mapper.CdcHiddenDiscrepancyMapper;
import uk.nhs.hee.tis.revalidation.integration.cdc.mapper.CdcHiddenDiscrepancyMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationType;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

/**
 * The utility class to help generate test data.
 */
@Component
public class CdcTestDataGenerator {

  private static final CdcHiddenDiscrepancyMapper cdcHiddenDiscrepancyMapper =
      new CdcHiddenDiscrepancyMapperImpl();
  public static final String GMC_REFERENCE_NUMBER_VAL = "111";
  public static final String DOCTOR_FIRST_NAME_VAL = "firstName";
  public static final String DOCTOR_LAST_NAME_VAL = "lastName";
  public static final UnderNotice UNDER_NOTICE_VAL = YES;
  public static final String SANCTION_VAL = "sanction";
  public static final RecommendationStatus DOCTOR_STATUS_VAL = SUBMITTED_TO_GMC;
  public static final String DESIGNATED_BODY_CODE_VAL = "designatedBodyCode";
  public static final String ADMIN_VAL = "admin";
  public static final Boolean EXISTS_IN_GMC_VAL = true;
  public static final String PROGRAMME_NAME_VAL = "Dev 4 pain";
  public static final String PROGRAMME_TYPE_VAL = "prog-type";
  public static final String PROGRAMME_OWNER_VAL = "squad";
  public static final String C_I = "ci";
  private static final String SUCCESSFUL_REQUEST_RESPONSE_CODE = "0";
  private static final String INTERNAL_ERROR_RESPONSE_CODE = "98";
  private static final String UPDATED_BY_GMC = "Updated by GMC";
  private static final String HIDDEN_REASON_VAL = "reason";

  public static final CdcDocumentKey DOCUMENT_KEY = CdcDocumentKey.builder().id("1234567").build();
  public static final CdcDocumentKey DOCUMENT_KEY_2 = CdcDocumentKey.builder().id("7654321")
      .build();
  public static final String CDC_DOC_JSON =
      """
          {
            "_id":{"_data":"01625a0706000001c001000001c000020042"},
            "operationType":"replace",
            "clusterTime":"Timestamp(1650067206, 448)",
            "ns":{"db":"revalidation","coll":"doctorsForDB"},
            "documentKey":{"_id":"1234567"},
            "fullDocument":{
                              "_id":"1234567","doctorFirstName":"First",
                              "doctorLastName":"Last",
                              "submissionDate":"2017-10-19 00:00:00",
                              "dateAdded":"2015-10-07 00:00:00",
                              "underNotice":"NO","sanction":"No",
                              "doctorStatus":"COMPLETED",
                              "lastUpdatedDate":"2022-04-15 00:00:00",
                              "designatedBodyCode":"1-AIIDWI",
                              "existsInGmc":false,
                              "_class":"uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"}
          }
          """;

  public static final String CDC_DOCDB_EVENT_JSON =
      """
          {
            "_id": {"_data": "016819321a00000001010000000000020042"},
            "clusterTime": {"$timestamp": {"t": 1746481690, "i": 1}},
            "documentKey": {"_id": "1234567"},
            "fullDocument": {
                              "_id": "1234567", "doctorFirstName": "AAA", "doctorLastName": "BBB",
                              "submissionDate": {"$date": "2024-08-05T00:00:00Z"},
                              "dateAdded": {"$date": "2015-10-07T00:00:00Z"}, "underNotice": "NO",
                              "sanction": "No", "doctorStatus": "DRAFT",
                              "lastUpdatedDate": {"$date": "2025-04-29T00:00:00Z"},
                              "gmcLastUpdatedDateTime": {"$date": "2025-04-29T00:00:54.956Z"},
                              "designatedBodyCode": "1-1RSSQ05", "existsInGmc": false,
                              "_class": "uk.nhs.hee.tis.revalidation.entity.DoctorsForDB"},
            "ns": {"db": "revalidation", "coll": "doctorsForDB"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [],
                                  "updatedFields": {"underNotice": "YES"}}
          }
          """;

  public static final String CDC_CONNECTION_LOG_EVENT_JSON =
      """
            {
              "_id": {"_data": "016819321a00000001010000000000020042"},
              "clusterTime": {"$timestamp": {"t": 1746481690, "i": 1}},
              "documentKey": {"_id": "1234567"},
              "fullDocument": {
                                "_id": "1234567",
                                "gmcId": "1234567",
                                "newDesignatedBodyCode": "1-1RSSQ05",
                                "previousDesignatedBodyCode": "1-AIIDWI",
                                "updatedBy": "admin",
                                "requestTime": {"$date": "2025-04-29T00:00:00Z"},
                                "responseCode": "0",
                                "_class": "uk.nhs.hee.tis.revalidation.entity.ConnectionLog"},
              "ns": {"db": "revalidation", "coll": "connectionLog"},
              "operationType": "insert"
            }
          """;

  public static final String CDC_RECOMMENDATION_EVENT_JSON =
      """
          {
            "_id": {"_data": "0168220a440000000b01000000000002d1b5"},
            "clusterTime": {"$timestamp": {"t": 1747061316, "i": 11}},
            "documentKey": {"_id": {"$oid": "1234567"}},
            "fullDocument": {
                              "_id": {"$oid": "1234567"},
                              "gmcNumber": "1234567", "recommendationType": "REVALIDATE",
                              "recommendationStatus": "SUBMITTED_TO_GMC",
                              "gmcSubmissionDate": {"$date": "2025-04-28T00:00:00Z"},
                              "comments": ["test"], "admin": "aaa.bbb@ccc.com",
                              "_class": "uk.nhs.hee.tis.revalidation.entity.Recommendation"},
            "ns": {"db": "revalidation", "coll": "recommendation"},
            "operationType": "update",
            "updateDescription": {"removedFields": [], "truncatedArrays": [],
                                  "updatedFields": {"recommendationStatus": "SUBMITTED_TO_GMC"}}}
          """;

  public static final String CDC_HIDDEN_DISCREPANCY_INSERT_EVENT =
      """
          {"_id": {"_data": "0169fdb35100000006010000000000053ab4"},
           "clusterTime": {"$timestamp": {"t": 1778234193, "i": 6}},
            "documentKey": {"_id": {"$oid": "1234567"}},
             "fullDocument": {"_id": {"$oid": "1234567"},
              "gmcId": "1234567", "hiddenForDesignatedBodyCode": "1-1RSSQ05",
               "hiddenBy": "test", "reason": "test", "hiddenDateTime":
                {"$date": "2026-05-08T09:56:33.453Z"}, "_class":
                 "uk.nhs.hee.tis.revalidation.connection.entity.HiddenDiscrepancy"},
                  "ns": {"db": "revalidation", "coll": "hiddenDiscrepancy"},
                   "operationType": "insert"}
          """;

  public static final String CDC_HIDDEN_DISCREPANCY_DELETE_EVENT =
      """
            {"_id": {"_data": "0169fcab1b00000003010000000000053ab4"},
             "clusterTime": {"$timestamp": {"t": 1778166555, "i": 3}},
              "documentKey": {"_id": {"$oid": "1234567"}},
               "ns": {"db": "revalidation", "coll": "hiddenDiscrepancy"},
                "operationType": "delete"}
          """;

  private static final DoctorsForDB doctorsForDB = DoctorsForDB.builder()
      .gmcReferenceNumber(GMC_REFERENCE_NUMBER_VAL)
      .doctorFirstName(DOCTOR_FIRST_NAME_VAL)
      .doctorLastName(DOCTOR_LAST_NAME_VAL)
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

  private static final DoctorsForDB doctorsForDBNullDbc = DoctorsForDB.builder()
      .gmcReferenceNumber(GMC_REFERENCE_NUMBER_VAL)
      .doctorFirstName(DOCTOR_FIRST_NAME_VAL)
      .doctorLastName(DOCTOR_LAST_NAME_VAL)
      .submissionDate(LocalDate.now())
      .dateAdded(LocalDate.now())
      .underNotice(UNDER_NOTICE_VAL)
      .sanction(SANCTION_VAL)
      .doctorStatus(DOCTOR_STATUS_VAL)
      .lastUpdatedDate(LocalDate.now())
      .admin(ADMIN_VAL)
      .existsInGmc(EXISTS_IN_GMC_VAL)
      .build();

  /**
   * Get a test instance of MasterDoctorView.
   *
   * @return MasterDoctorView test instance
   */
  public static MasterDoctorView getTestMasterDoctorView() {
    return MasterDoctorView.builder()
        .id(DOCUMENT_KEY.getId())
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
   * Get a test instance of MasterDoctorView.
   *
   * @return MasterDoctorView test instance with hidden discrepancies
   */
  public static MasterDoctorView getTestMasterDoctorViewWithHidden() {
    return MasterDoctorView.builder()
        .id(DOCUMENT_KEY.getId())
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
        .hiddenDiscrepancies(List.of(
            cdcHiddenDiscrepancyMapper.toEntity(
                getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY).getFullDocument())
        ))
        .build();
  }

  /**
   * Get a test instance of MasterDoctorView.
   *
   * @return MasterDoctorView test instance with hidden discrepancies
   */
  public static MasterDoctorView getTestMasterDoctorViewWithMultipleHidden() {
    var cdcDocumentDtoLists = List.of(
        getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY).getFullDocument(),
        getCdcHiddenDiscrepancyInsertCdcDocumentDto(DOCUMENT_KEY_2).getFullDocument());
    var hiddenDiscrepancies = cdcHiddenDiscrepancyMapper.toEntityList(cdcDocumentDtoLists);

    return MasterDoctorView.builder()
        .id(DOCUMENT_KEY.getId())
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
        .hiddenDiscrepancies(hiddenDiscrepancies)
        .build();
  }

  /**
   * Get a test instance of an insert DoctorsForDb CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcDoctor test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorInsertCdcDocumentDto() {
    return new CdcDocumentDto<DoctorsForDB>(OperationType.INSERT.getValue(), doctorsForDB,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an replace DoctorsForDb CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcDoctor test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorReplaceCdcDocumentDto() {
    return new CdcDocumentDto<DoctorsForDB>(OperationType.REPLACE.getValue(), doctorsForDB,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an update DoctorsForDb CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcDoctor test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorUpdateCdcDocumentDto() {
    return new CdcDocumentDto<DoctorsForDB>(OperationType.UPDATE.getValue(), doctorsForDB,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationInsertCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .outcome(RecommendationGmcOutcome.APPROVED)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.INSERT.getValue(), recommendation,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto with a null outcome.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation> getRecommendationInsertCdcDocumentDtoNullOutcome() {
    Recommendation recommendation = Recommendation.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.INSERT.getValue(), recommendation,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert CdcRecommendation CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcRecommendation insert test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationReplaceCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new CdcDocumentDto<Recommendation>(OperationType.REPLACE.getValue(), recommendation,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an unsupported doctor change operation.
   *
   * @return CdcDocumentDto CdcDoctor unsupported test instance
   */
  public static CdcDocumentDto<DoctorsForDB> getCdcDoctorUnsupportedCdcDocumentDto() {
    DoctorsForDB doctorsForDb = DoctorsForDB.builder().build();

    return new CdcDocumentDto<DoctorsForDB>(OperationType.DROP.getValue(), doctorsForDb,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an unsupported recommendation change operation.
   *
   * @return CdcDocumentDto CdcRecommendation unsupported test instance
   */
  public static CdcDocumentDto<Recommendation> getCdcRecommendationUnsupportedCdcDocumentDto() {
    Recommendation recommendation = Recommendation.builder().build();

    return new CdcDocumentDto<Recommendation>(OperationType.DROP.getValue(), recommendation,
        DOCUMENT_KEY);
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
        .build();
  }

  /**
   * Get a test instance of an insert DoctorsForDb.
   *
   * @return DoctorsForDB test instance
   */
  public static DoctorsForDB getCdcDoctor() {
    return doctorsForDB;
  }

  /**
   * Get a test instance of a DoctorsForDb Object with a null designated body code.
   *
   * @return DoctorsForDB null DBC test instance
   */
  public static DoctorsForDB getCdcDoctorNullDbc() {
    return doctorsForDBNullDbc;
  }

  /**
   * Get a test instance of an insert CdcConnectionLog CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcConnectionLog insert test instance
   */
  public static CdcDocumentDto<ConnectionLog> getCdcConnectionLogInsertCdcDocumentDto() {
    ConnectionLog connectionLog = ConnectionLog.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcId(GMC_REFERENCE_NUMBER_VAL)
        .requestTime(LocalDateTime.now())
        .updatedBy(ADMIN_VAL)
        .responseCode(SUCCESSFUL_REQUEST_RESPONSE_CODE)
        .build();

    return new CdcDocumentDto<ConnectionLog>(OperationType.INSERT.getValue(), connectionLog,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert CdcConnectionLog CdcDocumentDto for a failed connection.
   *
   * @return CdcDocumentDto CdcConnectionLog insert test instance
   */
  public static CdcDocumentDto<ConnectionLog> getCdcUnsuccessfulConnectionCdcDocumentDto() {
    ConnectionLog connectionLog = ConnectionLog.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcId(GMC_REFERENCE_NUMBER_VAL)
        .requestTime(LocalDateTime.now())
        .updatedBy(ADMIN_VAL)
        .responseCode(INTERNAL_ERROR_RESPONSE_CODE)
        .build();

    return new CdcDocumentDto<ConnectionLog>(OperationType.INSERT.getValue(), connectionLog,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert CdcConnectionLog CdcDocumentDto for an external connection.
   *
   * @return CdcDocumentDto CdcConnectionLog insert test instance
   */
  public static CdcDocumentDto<ConnectionLog> getCdcGmcExternalConnectionCdcDocumentDto() {
    ConnectionLog connectionLog = ConnectionLog.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcId(GMC_REFERENCE_NUMBER_VAL)
        .requestTime(LocalDateTime.now())
        .updatedBy(UPDATED_BY_GMC)
        .build();

    return new CdcDocumentDto<ConnectionLog>(OperationType.INSERT.getValue(), connectionLog,
        DOCUMENT_KEY);
  }

  /**
   * Get a test instance of an insert HiddenDiscrepancy CdcDocumentDto.
   *
   * @return CdcDocumentDto HiddenDiscrepancy insert test instance
   */
  public static CdcDocumentDto<CdcHiddenDiscrepancyDto>
      getCdcHiddenDiscrepancyInsertCdcDocumentDto(CdcDocumentKey key) {
    CdcHiddenDiscrepancyDto hiddenDiscrepancy = CdcHiddenDiscrepancyDto.builder()
        .id(key.getId())
        .gmcId(GMC_REFERENCE_NUMBER_VAL)
        .hiddenDateTime(LocalDateTime.now())
        .hiddenBy(ADMIN_VAL)
        .hiddenForDesignatedBodyCode(DESIGNATED_BODY_CODE_VAL + key)
        .reason(HIDDEN_REASON_VAL)
        .build();

    return new CdcDocumentDto<>(OperationType.INSERT.getValue(),
        hiddenDiscrepancy, key);
  }

  /**
   * Get a test instance of a deleted CdcHiddenDiscrepancyDto CdcDocumentDto.
   *
   * @return CdcDocumentDto CdcHiddenDiscrepancyDto deleted test instance
   */
  public static CdcDocumentDto<CdcHiddenDiscrepancyDto>
      getCdcHiddenDiscrepancyDeleteCdcDocumentDto() {
    CdcHiddenDiscrepancyDto hiddenDiscrepancy = CdcHiddenDiscrepancyDto.builder()
        .id(DOCUMENT_KEY.getId())
        .gmcId(GMC_REFERENCE_NUMBER_VAL)
        .hiddenDateTime(LocalDateTime.now())
        .hiddenBy(ADMIN_VAL)
        .hiddenForDesignatedBodyCode(DESIGNATED_BODY_CODE_VAL)
        .reason(HIDDEN_REASON_VAL)
        .build();

    return new CdcDocumentDto<>(OperationType.DELETE.getValue(),
        hiddenDiscrepancy, DOCUMENT_KEY);
  }

}
