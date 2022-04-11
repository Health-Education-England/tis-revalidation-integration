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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.model.changestream.UpdateDescription;
import org.bson.*;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationType;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

import java.time.Instant;
import java.time.LocalDate;

import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.*;
import static uk.nhs.hee.tis.revalidation.integration.cdc.RecommendationConstants.OUTCOME;
import static uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus.DRAFT;
import static uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus.SUBMITTED_TO_GMC;
import static uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice.YES;
import static uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome.APPROVED;

@Component
public class CdcTestDataGenerator {

  public static final String GMC_REFERENCE_NUMBER_VAL = "111";
  public static final String DOCTOR_FIRST_NAME_VAL = "firstName";
  public static final String DOCTOR_LAST_NAME_VAL = "lastName";
  public static final BsonDateTime SUBMISSION_DATE_VAL = new BsonDateTime(Instant.now().getEpochSecond());
  public static final BsonDateTime DATE_ADDED_VAL = new BsonDateTime(Instant.now().getEpochSecond());
  public static final UnderNotice UNDER_NOTICE_VAL = YES;
  public static final String SANCTION_VAL = "sanction";
  public static final RecommendationStatus DOCTOR_STATUS_VAL = SUBMITTED_TO_GMC;
  public static final BsonDateTime LAST_UPDATED_DATE_VAL = new BsonDateTime(Instant.now().getEpochSecond());
  public static final String DESIGNATED_BODY_CODE_VAL = "designatedBodyCode";
  public static final String ADMIN_VAL = "admin";
  public static final Boolean EXISTS_IN_GMC_VAL = true;

  private static final String doctorUpdateJson = "";
  private static final String recommendationUpdateJson = "";

  public static MasterDoctorView getTestMasterDoctorView() {
    return MasterDoctorView.builder()
        .id("1")
        .tcsPersonId(1L)
        .gmcReferenceNumber(GMC_REFERENCE_NUMBER_VAL)
        .doctorFirstName("old"+DOCTOR_FIRST_NAME_VAL)
        .doctorLastName("old"+DOCTOR_LAST_NAME_VAL)
        .submissionDate(LocalDate.now())
        .designatedBody("old"+DESIGNATED_BODY_CODE_VAL)
        .tisStatus(DRAFT)
        .lastUpdatedDate(LocalDate.now())
        .admin("old"+ADMIN)
        .existsInGmc(false)
        .build();
  }

  public static ChangeStreamDocument<DoctorsForDB> getDoctorInsertChangeStreamDocument() {
    DoctorsForDB doctorsForDB = DoctorsForDB.builder()
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

    return new ChangeStreamDocument<>(
        OperationType.INSERT,
        BsonDocument.parse("{}"),
        null,
        null,
        doctorsForDB,
        null,
        null,
        null,
        null,
        null
    );
  }

  public static ChangeStreamDocument<Recommendation> getRecommendationInsertChangeStreamDocument() {
    Recommendation recommendation = Recommendation.builder()
        .id("1")
        .gmcNumber(GMC_REFERENCE_NUMBER_VAL)
        .recommendationType(RecommendationType.REVALIDATE)
        .recommendationStatus(DRAFT)
        .gmcSubmissionDate(LocalDate.now().plusMonths(6))
        .admin(ADMIN_VAL)
        .build();

    return new ChangeStreamDocument<>(
        OperationType.INSERT,
        BsonDocument.parse("{}"),
        null,
        null,
        recommendation,
        null,
        null,
        null,
        null,
        null
    );
  }

  public static ChangeStreamDocument<DoctorsForDB> getDoctorUpdateChangeStreamDocument() {

    var updatesBson = new BsonDocument();
    updatesBson.put(DOCTOR_FIRST_NAME, new BsonString(DOCTOR_FIRST_NAME_VAL));
    updatesBson.put(DOCTOR_LAST_NAME, new BsonString(DOCTOR_LAST_NAME_VAL));
    updatesBson.put(SUBMISSION_DATE, SUBMISSION_DATE_VAL);
    updatesBson.put(UNDER_NOTICE, new BsonString(UNDER_NOTICE_VAL.value()));
    updatesBson.put(DOCTOR_STATUS, new BsonString(DOCTOR_STATUS_VAL.toString()));
    updatesBson.put(LAST_UPDATED_DATE, new BsonString(LAST_UPDATED_DATE));
    updatesBson.put(DESIGNATED_BODY_CODE, new BsonString(DESIGNATED_BODY_CODE_VAL));
    updatesBson.put(ADMIN, new BsonString(ADMIN_VAL));
    updatesBson.put(EXISTS_IN_GMC, new BsonBoolean(EXISTS_IN_GMC_VAL));

    return new ChangeStreamDocument<DoctorsForDB>(
        OperationType.UPDATE,
        BsonDocument.parse("{}"),
        null,
        null,
        null,
        null,
        null,
        new UpdateDescription(null, updatesBson),
        null,
        null
    );
  }

  public static ChangeStreamDocument<Recommendation> getRecommendationUpdateChangeStreamDocument() {

    var updatesBson = new BsonDocument();
    updatesBson.put(OUTCOME, new BsonString(APPROVED.getOutcome()));
    updatesBson.put(LAST_UPDATED_DATE, new BsonDateTime(Instant.now().getEpochSecond()));

    return new ChangeStreamDocument<Recommendation>(
        OperationType.UPDATE,
        BsonDocument.parse("{}"),
        null,
        null,
        null,
        null,
        null,
        new UpdateDescription(null, updatesBson),
        null,
        null
    );
  }

  public static ChangeStreamDocument<DoctorsForDB> getDoctorUnsupportedChangeStreamDocument() {
    DoctorsForDB doctorsForDB = DoctorsForDB.builder().build();

    return new ChangeStreamDocument<>(
        OperationType.DROP,
        BsonDocument.parse("{}"),
        null,
        null,
        doctorsForDB,
        null,
        null,
        null,
        null,
        null
    );
  }

  public static ChangeStreamDocument<Recommendation> getRecommendationUnsupportedChangeStreamDocument() {
    Recommendation doctorsForDB = Recommendation.builder().build();

    return new ChangeStreamDocument<>(
        OperationType.DROP,
        BsonDocument.parse("{}"),
        null,
        null,
        doctorsForDB,
        null,
        null,
        null,
        null,
        null
    );
  }

}
