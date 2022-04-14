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

package uk.nhs.hee.tis.revalidation.integration.cdc.service.helper;

import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.ADMIN;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DESIGNATED_BODY_CODE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_FIRST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_LAST_NAME;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.DOCTOR_STATUS;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.EXISTS_IN_GMC;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.LAST_UPDATED_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.SUBMISSION_DATE;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.UNDER_NOTICE;

import org.bson.BsonDocument;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Component
public class CdcDoctorFieldUpdateHelper implements CdcFieldUpdateHelper {
  @Override
  public void updateField(MasterDoctorView masterDoctorView, String key, BsonDocument updates) {
    switch (key) {
      case DOCTOR_FIRST_NAME:
        masterDoctorView.setDoctorFirstName(updates.getString(DOCTOR_FIRST_NAME).getValue());
        break;
      case DOCTOR_LAST_NAME:
        masterDoctorView.setDoctorLastName(updates.getString(DOCTOR_LAST_NAME).getValue());
        break;
      case SUBMISSION_DATE:
        masterDoctorView.setSubmissionDate(
            getLocalDateFromBsonDateTime(updates.getDateTime(SUBMISSION_DATE))
        );
        break;
      case UNDER_NOTICE:
        masterDoctorView.setUnderNotice(
            UnderNotice.fromString(updates.getString(UNDER_NOTICE).getValue())
        );
        break;
      case DOCTOR_STATUS:
        masterDoctorView.setTisStatus(
            RecommendationStatus.valueOf(updates.getString(DOCTOR_STATUS).getValue())
        );
        break;
      case LAST_UPDATED_DATE:
        masterDoctorView.setLastUpdatedDate(
            getLocalDateFromBsonDateTime(updates.getDateTime(LAST_UPDATED_DATE))
        );
        break;
      case DESIGNATED_BODY_CODE:
        masterDoctorView.setDesignatedBody(updates.getString(DESIGNATED_BODY_CODE).getValue());
        break;
      case ADMIN:
        masterDoctorView.setAdmin(updates.getString(ADMIN).getValue());
        break;
      case EXISTS_IN_GMC:
        masterDoctorView.setExistsInGmc(
            updates.getBoolean(EXISTS_IN_GMC).getValue()
        );
        break;
      default:
        break;
    }
  }
}
