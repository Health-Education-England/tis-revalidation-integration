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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class MasterDoctorViewMapperTest {

  @InjectMocks
  MasterDoctorViewMapperImpl masterDoctorViewMapper;

  private MasterDoctorView currentDoctorView;

  @BeforeEach
  void setUp() {

    currentDoctorView = MasterDoctorView.builder()
        .id("1a2b3c")
        .tcsPersonId(1001L)
        .gmcReferenceNumber("gmcReferenceNumber")
        .doctorFirstName("doctorFirstName")
        .doctorLastName("doctorLastName")
        .programmeName("programmeName")
        .membershipType("membershipType")
        .designatedBody("designatedBody")
        .tcsDesignatedBody("tcsDesignatedBody")
        .programmeOwner("programmeOwner")
        .connectionStatus("Yes")
        .build();
  }

  @Test
  void shouldUpdateMasterDoctorViewsToNewValues() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setProgrammeName("programmeName_new");
    dataToSave.setMembershipType("membershipType_new");
    dataToSave.setTcsDesignatedBody("tcsDesignatedBody_new");
    dataToSave.setProgrammeOwner("programmeOwner_new");

    MasterDoctorView result = masterDoctorViewMapper.updateMasterDoctorView(
        dataToSave, currentDoctorView);

    assertThat(result.getId(), is("1a2b3c"));
    assertThat(result.getTcsPersonId(), is(1001L));

    //mapper will make sure new values are populated
    assertThat(result.getProgrammeName(), is("programmeName_new"));
    assertThat(result.getMembershipType(), is("membershipType_new"));
    assertThat(result.getTcsDesignatedBody(), is("tcsDesignatedBody_new"));
    assertThat(result.getProgrammeOwner(), is("programmeOwner_new"));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is("gmcReferenceNumber"));
    assertThat(result.getDoctorFirstName(), is("doctorFirstName"));
    assertThat(result.getDoctorLastName(), is("doctorLastName"));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWithNewFields() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setAdmin("admin");
    dataToSave.setGmcStatus("gmcStatus");
    dataToSave.setTisStatus(RecommendationStatus.COMPLETED);

    MasterDoctorView result = masterDoctorViewMapper.updateMasterDoctorView(
        dataToSave, currentDoctorView);

    assertThat(result.getId(), is("1a2b3c"));
    assertThat(result.getTcsPersonId(), is(1001L));

    //mapper will make sure new fields are populated
    assertThat(result.getAdmin(), is("admin"));
    assertThat(result.getGmcStatus(), is("gmcStatus"));
    assertThat(result.getTisStatus(), is(RecommendationStatus.COMPLETED));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is("gmcReferenceNumber"));
    assertThat(result.getDoctorFirstName(), is("doctorFirstName"));
    assertThat(result.getDoctorLastName(), is("doctorLastName"));
    assertThat(result.getProgrammeOwner(), is("programmeOwner"));
  }

  @Test
  void shouldNotUpdateMasterDoctorFieldsWhenFieldsNull() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setProgrammeName(null);
    dataToSave.setMembershipType(null);
    dataToSave.setTcsDesignatedBody(null);
    dataToSave.setProgrammeOwner(null);

    MasterDoctorView result = masterDoctorViewMapper.updateMasterDoctorView(
        dataToSave, currentDoctorView);

    assertThat(result.getId(), is("1a2b3c"));
    assertThat(result.getTcsPersonId(), is(1001L));

    //mapper will make sure null fields will not override the existing values
    assertThat(result.getProgrammeName(), is("programmeName"));
    assertThat(result.getMembershipType(), is("membershipType"));
    assertThat(result.getTcsDesignatedBody(), is("tcsDesignatedBody"));
    assertThat(result.getProgrammeOwner(), is("programmeOwner"));

    //other fields will remain same
    assertThat(result.getGmcReferenceNumber(), is("gmcReferenceNumber"));
    assertThat(result.getDoctorFirstName(), is("doctorFirstName"));
    assertThat(result.getDoctorLastName(), is("doctorLastName"));
  }

  @Test
  void shouldNotUpdateMasterDoctorViewsWhenNull() {
    MasterDoctorView dataToSave = null;
    MasterDoctorView result = masterDoctorViewMapper.updateMasterDoctorView(
        dataToSave, currentDoctorView);

    assertThat(result, nullValue());
  }
}
