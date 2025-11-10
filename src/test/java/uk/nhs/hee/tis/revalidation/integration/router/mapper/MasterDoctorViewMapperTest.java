/*
 * The MIT License (MIT)
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

package uk.nhs.hee.tis.revalidation.integration.router.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class MasterDoctorViewMapperTest {

  private static final String ID = "1a2b3c";
  private static final long TIS_ID = 1001L;
  private static final String GMC_REFERENCE_NUMBER = "gmcReferenceNumber";
  private static final String DOCTOR_FIRST_NAME = "doctorFirstName";
  private static final String DOCTOR_LAST_NAME = "doctorLastName";
  private static final String PROGRAMME_NAME = "programmeName";
  private static final String MEMBERSHIP_TYPE = "membershipType";
  private static final String DESIGNATED_BODY = "designatedBody";
  private static final String TCS_DESIGNATED_BODY = "tcsDesignatedBody";
  private static final String PROGRAMME_OWNER = "programmeOwner";
  private static final String CONNECTION_YES = "Yes";
  private static final String POSTFIX_NEW = "_new";
  private static final String ADMIN = "admin";
  private static final String GMC_STATUS = "gmcStatus";
  private static final LocalDate SUBMISSION_DATE = LocalDate.of(2025, 11, 10);
  private static final LocalDate LAST_UPDATED_DATE = LocalDate.of(2025, 11, 10);
  private static final RecommendationStatus DOCTOR_STATUS = RecommendationStatus.READY_TO_REVIEW;
  private static final String DESIGNATED_BODY_CODE = "dbc";
  private static final UnderNotice UNDER_NOTICE = UnderNotice.NO;
  private static final Boolean EXISTS_IN_GMC = true;

  @InjectMocks
  MasterDoctorViewMapperImpl masterDoctorViewMapper;

  private MasterDoctorView currentDoctorView;

  @BeforeEach
  void setUp() {
    currentDoctorView = MasterDoctorView.builder()
        .id(ID)
        .tcsPersonId(TIS_ID)
        .gmcReferenceNumber(GMC_REFERENCE_NUMBER)
        .doctorFirstName(DOCTOR_FIRST_NAME)
        .doctorLastName(DOCTOR_LAST_NAME)
        .programmeName(PROGRAMME_NAME)
        .membershipType(MEMBERSHIP_TYPE)
        .designatedBody(DESIGNATED_BODY)
        .tcsDesignatedBody(TCS_DESIGNATED_BODY)
        .programmeOwner(PROGRAMME_OWNER)
        .build();
  }

  @Test
  void shouldUpdateMasterDoctorViewsToNewValues() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setProgrammeName(PROGRAMME_NAME + POSTFIX_NEW);
    dataToSave.setMembershipType(MEMBERSHIP_TYPE + POSTFIX_NEW);
    dataToSave.setTcsDesignatedBody(TCS_DESIGNATED_BODY + POSTFIX_NEW);
    dataToSave.setProgrammeOwner(PROGRAMME_OWNER + POSTFIX_NEW);

    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView(dataToSave, currentDoctorView);

    assertThat(result.getId(), is(ID));
    assertThat(result.getTcsPersonId(), is(TIS_ID));

    //mapper will make sure new values are populated
    assertThat(result.getProgrammeName(), is(PROGRAMME_NAME + POSTFIX_NEW));
    assertThat(result.getMembershipType(), is(MEMBERSHIP_TYPE + POSTFIX_NEW));
    assertThat(result.getTcsDesignatedBody(), is(TCS_DESIGNATED_BODY + POSTFIX_NEW));
    assertThat(result.getProgrammeOwner(), is(PROGRAMME_OWNER + POSTFIX_NEW));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is(GMC_REFERENCE_NUMBER));
    assertThat(result.getDoctorFirstName(), is(DOCTOR_FIRST_NAME));
    assertThat(result.getDoctorLastName(), is(DOCTOR_LAST_NAME));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithNewFields() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setAdmin(ADMIN);
    dataToSave.setGmcStatus(GMC_STATUS);
    dataToSave.setTisStatus(RecommendationStatus.COMPLETED);

    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView(dataToSave, currentDoctorView);

    assertThat(result.getId(), is(ID));
    assertThat(result.getTcsPersonId(), is(TIS_ID));

    //mapper will make sure new fields are populated
    assertThat(result.getAdmin(), is(ADMIN));
    assertThat(result.getGmcStatus(), is(GMC_STATUS));
    assertThat(result.getTisStatus(), is(RecommendationStatus.COMPLETED));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is(GMC_REFERENCE_NUMBER));
    assertThat(result.getDoctorFirstName(), is(DOCTOR_FIRST_NAME));
    assertThat(result.getDoctorLastName(), is(DOCTOR_LAST_NAME));
    assertThat(result.getProgrammeOwner(), is(PROGRAMME_OWNER));
  }

  @Test
  void shouldNotUpdateMasterDoctorFieldsWhenFieldsNull() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setProgrammeName(null);
    dataToSave.setMembershipType(null);
    dataToSave.setTcsDesignatedBody(null);
    dataToSave.setProgrammeOwner(null);

    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView(dataToSave, currentDoctorView);

    assertThat(result.getId(), is(ID));
    assertThat(result.getTcsPersonId(), is(TIS_ID));

    //mapper will make sure null fields will not override the existing values
    assertThat(result.getProgrammeName(), is(PROGRAMME_NAME));
    assertThat(result.getMembershipType(), is(MEMBERSHIP_TYPE));
    assertThat(result.getTcsDesignatedBody(), is(TCS_DESIGNATED_BODY));
    assertThat(result.getProgrammeOwner(), is(PROGRAMME_OWNER));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is(GMC_REFERENCE_NUMBER));
    assertThat(result.getDoctorFirstName(), is(DOCTOR_FIRST_NAME));
    assertThat(result.getDoctorLastName(), is(DOCTOR_LAST_NAME));
  }

  @Test
  void shouldNotUpdateMasterDoctorViewsWhenNull() {
    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView((MasterDoctorView) null, currentDoctorView);

    assertThat(result, nullValue());
  }

  @Test
  void shouldUpdateNonNullFields() {
    ConnectionInfoDto source = CdcTestDataGenerator.getConnectionInfo();

    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView(source, currentDoctorView);

    assertThat(result, notNullValue());
    assertThat(result.getGmcReferenceNumber(), is(source.getGmcReferenceNumber()));
    assertThat(result.getTcsPersonId(), is(source.getTcsPersonId()));
  }

  @Test
  void shouldUpdateNullTcsProgrammeFields() {
    ConnectionInfoDto source = ConnectionInfoDto.builder().build();
    source.setGmcReferenceNumber(GMC_REFERENCE_NUMBER);

    MasterDoctorView result = masterDoctorViewMapper
        .updateMasterDoctorView(source, currentDoctorView);

    assertThat(result.getGmcReferenceNumber(), is(source.getGmcReferenceNumber()));
    assertThat(result.getTcsPersonId(), is(TIS_ID));
    assertThat(result.getProgrammeName(), nullValue());
    assertThat(result.getProgrammeOwner(), nullValue());
    assertThat(result.getCurriculumEndDate(), nullValue());
    assertThat(result.getMembershipStartDate(), nullValue());
    assertThat(result.getMembershipStartDate(), nullValue());
    assertThat(result.getMembershipEndDate(), nullValue());
    assertThat(result.getTcsDesignatedBody(), nullValue());
  }

  @Test
  void testDoctorToEsDoc_shouldReturnNullWhenInputIsNull() {
    // when
    Map<String, Object> result = masterDoctorViewMapper.doctorToEsDoc(null);

    // then
    assertNull(result);
  }

  @Test
  void testDoctorToEsDoc_shouldMapAllExpectedFields() {
    // given
    DoctorsForDB doctor = new DoctorsForDB();
    doctor.setDoctorFirstName(DOCTOR_FIRST_NAME);
    doctor.setDoctorLastName(DOCTOR_LAST_NAME);
    doctor.setGmcReferenceNumber(GMC_REFERENCE_NUMBER);
    doctor.setSubmissionDate(SUBMISSION_DATE);
    doctor.setDoctorStatus(DOCTOR_STATUS);
    doctor.setDesignatedBodyCode(DESIGNATED_BODY_CODE);
    doctor.setAdmin(ADMIN);
    doctor.setLastUpdatedDate(LAST_UPDATED_DATE);
    doctor.setUnderNotice(UNDER_NOTICE);
    doctor.setExistsInGmc(EXISTS_IN_GMC);

    // when
    Map<String, Object> result = masterDoctorViewMapper.doctorToEsDoc(doctor);

    // then
    assertNotNull(result);
    assertEquals(DOCTOR_FIRST_NAME, result.get("doctorFirstName"));
    assertEquals(DOCTOR_LAST_NAME, result.get("doctorLastName"));
    assertEquals(GMC_REFERENCE_NUMBER, result.get("gmcReferenceNumber"));
    assertEquals(SUBMISSION_DATE, result.get("submissionDate"));
    assertEquals(DOCTOR_STATUS, result.get("tisStatus"));
    assertEquals(DESIGNATED_BODY_CODE, result.get("designatedBody"));
    assertEquals(ADMIN, result.get("admin"));
    assertEquals(LAST_UPDATED_DATE, result.get("lastUpdatedDate"));
    assertEquals(UNDER_NOTICE, result.get("underNotice"));
    assertEquals(EXISTS_IN_GMC, result.get("existsInGmc"));
  }
}
