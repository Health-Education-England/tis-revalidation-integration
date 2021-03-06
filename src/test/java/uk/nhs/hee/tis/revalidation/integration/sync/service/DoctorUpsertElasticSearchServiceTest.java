/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.sync.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.sync.repository.MasterDoctorElasticSearchRepository;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class DoctorUpsertElasticSearchServiceTest {

  @Mock
  private MasterDoctorElasticSearchRepository repository;

  private DoctorUpsertElasticSearchService service;
  private MasterDoctorView currentDoctorView, dataToSave;

  @BeforeEach
  void setUp() {
    service = new DoctorUpsertElasticSearchService(repository, new MasterDoctorViewMapperImpl());
    currentDoctorView = MasterDoctorView.builder()
        .tcsPersonId(1001L)
        .gmcReferenceNumber("56789")
        .doctorFirstName("AAAAA")
        .doctorLastName("BBBB")
        .submissionDate(LocalDate.now())
        .programmeName("Medicine")
        .membershipType("Visitor")
        .designatedBody("EoE")
        .tcsDesignatedBody("KSS")
        .programmeOwner("East of England")
        .connectionStatus("Yes")
        .build();

    dataToSave = MasterDoctorView.builder()
        .tcsPersonId(1001L)
        .gmcReferenceNumber("56789")
        .doctorFirstName("AAAAA")
        .doctorLastName("BBBB")
        .submissionDate(LocalDate.now())
        .programmeName("")
        .membershipType("")
        .designatedBody("EoE")
        .tcsDesignatedBody("")
        .programmeOwner("")
        .connectionStatus("Yes")
        .build();
  }

  @Test
  void shouldUpdateMasterDoctorViewsWhenRecordIsFoundInEs() {
    List<MasterDoctorView> recordsAlreadyInEs = new ArrayList<>();
    //the id is needed as it will be the record already in the ES
    currentDoctorView.setId("1a2b3c");
    recordsAlreadyInEs.add(currentDoctorView);

    when(repository
        .findByGmcReferenceNumberAndTcsPersonId(dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId()))
        .thenReturn(recordsAlreadyInEs);

    //id which is unique needs to be set to avoid duplicate rows while updating the record in ES
    dataToSave.setId(recordsAlreadyInEs.get(0).getId());

    //this will lead to updateMasterDoctorViews() as record is there already in the ES
    service.populateMasterIndex(dataToSave);

    assertThat(dataToSave.getId(), is("1a2b3c"));
    assertThat(dataToSave.getTcsPersonId(), is(1001L));

    //mapper will make sure all the TIS data fields are populated
    assertThat(dataToSave.getProgrammeName(), is("Medicine"));
    assertThat(dataToSave.getMembershipType(), is("Visitor"));
    assertThat(dataToSave.getTcsDesignatedBody(), is("KSS"));
    assertThat(dataToSave.getProgrammeOwner(), is("East of England"));

    //gmc fields will remain same
    assertThat(dataToSave.getGmcReferenceNumber(), is("56789"));
    assertThat(dataToSave.getDoctorFirstName(), is("AAAAA"));
    assertThat(dataToSave.getDoctorLastName(), is("BBBB"));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWhenRecordIsFoundInEsPersonIdNull() {
    List<MasterDoctorView> recordsAlreadyInEs = new ArrayList<>();
    //the id is needed as it will be the record already in the ES
    currentDoctorView.setId("1a2b3c");
    recordsAlreadyInEs.add(currentDoctorView);

    dataToSave.setTcsPersonId(null);
    when(repository
        .findByGmcReferenceNumber(dataToSave.getGmcReferenceNumber()))
        .thenReturn(recordsAlreadyInEs);

    //id which is unique needs to be set to avoid duplicate rows while updating the record in ES
    dataToSave.setId(recordsAlreadyInEs.get(0).getId());

    //this will lead to updateMasterDoctorViews() as record is there already in the ES
    service.populateMasterIndex(dataToSave);

    assertThat(dataToSave.getId(), is("1a2b3c"));
    assertThat(dataToSave.getTcsPersonId(), is(1001L));

    //mapper will make sure all the TIS data fields are populated
    assertThat(dataToSave.getProgrammeName(), is("Medicine"));
    assertThat(dataToSave.getMembershipType(), is("Visitor"));
    assertThat(dataToSave.getTcsDesignatedBody(), is("KSS"));
    assertThat(dataToSave.getProgrammeOwner(), is("East of England"));

    //gmc fields will remain same
    assertThat(dataToSave.getGmcReferenceNumber(), is("56789"));
    assertThat(dataToSave.getDoctorFirstName(), is("AAAAA"));
    assertThat(dataToSave.getDoctorLastName(), is("BBBB"));
  }

  @Test
  void shouldUpdateMasterDoctorViewsWhenRecordIsFoundInEsGmcIdNull() {
    List<MasterDoctorView> recordsAlreadyInEs = new ArrayList<>();
    //the id is needed as it will be the record already in the ES
    currentDoctorView.setId("1a2b3c");
    recordsAlreadyInEs.add(currentDoctorView);

    dataToSave.setGmcReferenceNumber(null);
    when(repository
        .findByTcsPersonId(dataToSave.getTcsPersonId()))
        .thenReturn(recordsAlreadyInEs);

    //id which is unique needs to be set to avoid duplicate rows while updating the record in ES
    dataToSave.setId(recordsAlreadyInEs.get(0).getId());

    //this will lead to updateMasterDoctorViews() as record is there already in the ES
    service.populateMasterIndex(dataToSave);

    assertThat(dataToSave.getId(), is("1a2b3c"));
    assertThat(dataToSave.getTcsPersonId(), is(1001L));

    //mapper will make sure all the TIS data fields are populated
    assertThat(dataToSave.getProgrammeName(), is("Medicine"));
    assertThat(dataToSave.getMembershipType(), is("Visitor"));
    assertThat(dataToSave.getTcsDesignatedBody(), is("KSS"));
    assertThat(dataToSave.getProgrammeOwner(), is("East of England"));

    //gmc fields will remain same
    assertThat(dataToSave.getDoctorFirstName(), is("AAAAA"));
    assertThat(dataToSave.getDoctorLastName(), is("BBBB"));
  }

  @Test
  void shouldAddMasterDoctorViewsWhenRecordIsNotInEs() {
    //this is an empty record list
    List<MasterDoctorView> recordsAlreadyInEs = new ArrayList<>();

    //this is a new record, so query will return nothing
    when(repository
        .findByGmcReferenceNumberAndTcsPersonId(dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId()))
        .thenReturn(recordsAlreadyInEs);

    //this will lead to addMasterDoctorViews() as no record found
    service.populateMasterIndex(dataToSave);

    verify(repository).save(dataToSave);

    assertThat(dataToSave.getGmcReferenceNumber(), is("56789"));
    assertThat(dataToSave.getDoctorFirstName(), is("AAAAA"));
    assertThat(dataToSave.getDoctorLastName(), is("BBBB"));
    assertThat(dataToSave.getSubmissionDate(), is(LocalDate.now()));
    assertThat(dataToSave.getDesignatedBody(), is("EoE"));
    assertThat(dataToSave.getConnectionStatus(), is("Yes"));

    //gmc doesn't know about TIS data
    assertThat(dataToSave.getProgrammeName(), is(""));
    assertThat(dataToSave.getMembershipType(), is(""));
    assertThat(dataToSave.getTcsDesignatedBody(), is(""));
    assertThat(dataToSave.getProgrammeOwner(), is(""));
  }
}
