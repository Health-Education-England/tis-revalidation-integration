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

package uk.nhs.hee.tis.revalidation.integration.sync.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.LocalDate;
import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class GmcDoctorMessageListenerTest {

  DoctorsForDB doctorsForDB;
  MasterDoctorView masterDoctorView;
  @InjectMocks
  private GmcDoctorMessageListener gmcDoctorMessageListener;
  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  @BeforeEach
  public void setUp() {
    doctorsForDB = DoctorsForDB.builder()
        .gmcReferenceNumber("101")
        .doctorFirstName("AAA")
        .doctorLastName("BBB")
        .submissionDate(LocalDate.now())
        .dateAdded(LocalDate.now())
        .underNotice(UnderNotice.NO)
        .sanction("sanc")
        .doctorStatus(RecommendationStatus.NOT_STARTED)
        .lastUpdatedDate(LocalDate.now())
        .designatedBodyCode("PQR")
        .admin("Reval Admin").build();

    masterDoctorView = MasterDoctorView.builder()
        .tcsPersonId(null)
        .gmcReferenceNumber("101")
        .doctorFirstName("AAA")
        .doctorLastName("BBB")
        .submissionDate(LocalDate.now())
        .programmeName("No Programme Name")
        .membershipType("No Membership Type")
        .designatedBody("DBC")
        .tcsDesignatedBody("No TCS DBC")
        .programmeOwner("No Programme Owner")
        .connectionStatus("Yes")
        .membershipStartDate(null)
        .membershipEndDate(null)
        .build();
  }

  @Test
  public void testMessagesAreReceivedFromSqsQueue() throws Exception {

    setField(gmcDoctorMessageListener, "sqsEndPoint", "sqsEndPoint");

    //method under test
    gmcDoctorMessageListener.getMessage(doctorsForDB);

    assertThat(doctorsForDB.getGmcReferenceNumber(), is("101"));
    assertThat(doctorsForDB.getDoctorFirstName(), is("AAA"));
    assertThat(doctorsForDB.getDoctorLastName(), is("BBB"));
    assertThat(doctorsForDB.getSubmissionDate(), is(LocalDate.now()));
    assertThat(doctorsForDB.getDateAdded(), is(LocalDate.now()));
    assertThat(doctorsForDB.getUnderNotice(), is(UnderNotice.NO));
    assertThat(doctorsForDB.getDesignatedBodyCode(), is("PQR"));

  }

}
