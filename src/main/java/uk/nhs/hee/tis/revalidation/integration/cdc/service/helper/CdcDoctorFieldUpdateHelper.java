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

import org.bson.BsonDocument;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.*;
import static uk.nhs.hee.tis.revalidation.integration.cdc.DoctorConstants.EXISTS_IN_GMC;

@Component
public class CdcDoctorFieldUpdateHelper extends CdcFieldUpdateHelper{
  @Override
  public void updateField(MasterDoctorView masterDoctorView, String key, BsonDocument updates) {
    switch(key) {
      case DOCTOR_FIRST_NAME:
        masterDoctorView.setDoctorFirstName(String.valueOf(updates.getString(DOCTOR_FIRST_NAME)));
        break;
      case DOCTOR_LAST_NAME:
        masterDoctorView.setDoctorLastName(String.valueOf(updates.getString(DOCTOR_LAST_NAME)));
        break;
      case SUBMISSION_DATE:
        masterDoctorView.setSubmissionDate(getLocalDateFromBsonDateTime(updates.getDateTime(SUBMISSION_DATE)));
        break;
      case UNDER_NOTICE:
        masterDoctorView.setUnderNotice(UnderNotice.valueOf(String.valueOf(updates.getString(UNDER_NOTICE))));
        break;
      case DOCTOR_STATUS:
        masterDoctorView.setTisStatus(RecommendationStatus.valueOf(String.valueOf(updates.getString(DOCTOR_STATUS))));
        break;
      case LAST_UPDATED_DATE:
        masterDoctorView.setLastUpdatedDate(getLocalDateFromBsonDateTime(updates.getDateTime(LAST_UPDATED_DATE)));
        break;
      case DESIGNATED_BODY_CODE:
        masterDoctorView.setDesignatedBody(String.valueOf(updates.getString(DESIGNATED_BODY_CODE)));
        break;
      case ADMIN:
        masterDoctorView.setAdmin(String.valueOf(updates.getString(ADMIN)));
        break;
      case EXISTS_IN_GMC:
        masterDoctorView.setExistsInGmc(Boolean.valueOf(String.valueOf((updates.getBoolean(EXISTS_IN_GMC)))));
        break;
      default:
        break;
    }
  }
}
