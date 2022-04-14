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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.service.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.ADMIN;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DESIGNATED_BODY_CODE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_FIRST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_LAST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_STATUS;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.EXISTS_IN_GMC;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.LAST_UPDATED_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.SUBMISSION_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.UNDER_NOTICE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.RecommendationConstants.OUTCOME;
import static uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus.SUBMITTED_TO_GMC;

import java.time.Instant;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.helper.CdcDoctorFieldUpdateHelper;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class CdcDoctorFieldUpdateHelperTest {
  @InjectMocks
  CdcDoctorFieldUpdateHelper helper;

  private BsonDocument stringUpdate;
  private BsonDocument dateUpdate;
  private BsonDocument booleanUpdate;


  @BeforeEach
  void setup() {
    stringUpdate = new BsonDocument();
    dateUpdate = new BsonDocument();
    booleanUpdate = new BsonDocument();
  }

  @Test
  void shouldUpdateDoctorFirstName() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = DOCTOR_FIRST_NAME;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setDoctorFirstName(value);
  }

  @Test
  void shouldUpdateDoctorLastName() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = DOCTOR_LAST_NAME;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setDoctorLastName(value);
  }

  @Test
  void shouldUpdateSubmissionDate() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = SUBMISSION_DATE;
    final var value = Instant.now().getEpochSecond();
    dateUpdate.put(key, new BsonDateTime(value));

    helper.updateField(masterDoctorView, key, dateUpdate);

    verify(masterDoctorView)
        .setSubmissionDate(
            helper.getLocalDateFromBsonDateTime(dateUpdate.getDateTime(SUBMISSION_DATE))
        );
  }

  @Test
  void shouldUpdateDoctorUnderNotice() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = UNDER_NOTICE;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setUnderNotice(UnderNotice.fromString(value));
  }

  @Test
  void shouldUpdateDoctorTisStatus() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = DOCTOR_STATUS;
    final var value = SUBMITTED_TO_GMC.toString();
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setTisStatus(RecommendationStatus.valueOf(value));
  }

  @Test
  void shouldUpdateLastUpdatedDate() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = LAST_UPDATED_DATE;
    final var value = Instant.now().getEpochSecond();
    dateUpdate.put(key, new BsonDateTime(value));

    helper.updateField(masterDoctorView, key, dateUpdate);

    verify(masterDoctorView)
        .setLastUpdatedDate(
            helper.getLocalDateFromBsonDateTime(dateUpdate.getDateTime(LAST_UPDATED_DATE))
        );
  }

  @Test
  void shouldUpdateDoctorDesignatedBodyCode() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = DESIGNATED_BODY_CODE;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setDesignatedBody(value);
  }

  @Test
  void shouldUpdateDoctorAdmin() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = ADMIN;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView).setAdmin(value);
  }

  @Test
  void shouldUpdateDoctorExistsInGmc() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = EXISTS_IN_GMC;
    final var value = true;
    booleanUpdate.put(key, new BsonBoolean(value));

    helper.updateField(masterDoctorView, key, booleanUpdate);

    verify(masterDoctorView).setExistsInGmc(value);
  }

  @Test
  void shouldNotUpdateOtherFields() {
    MasterDoctorView masterDoctorView = Mockito.spy(MasterDoctorView.builder().build());
    final var key = OUTCOME;
    final var value = "value";
    stringUpdate.put(key, new BsonString(value));

    helper.updateField(masterDoctorView, key, stringUpdate);

    verify(masterDoctorView, never()).setGmcStatus(any());
  }
}
