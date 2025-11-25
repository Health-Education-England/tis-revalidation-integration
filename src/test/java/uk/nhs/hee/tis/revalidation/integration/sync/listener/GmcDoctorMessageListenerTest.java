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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.enums.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapper;
import uk.nhs.hee.tis.revalidation.integration.router.mapper.MasterDoctorViewMapperImpl;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class GmcDoctorMessageListenerTest {

  private IndexSyncMessage message;
  private static final String GMC_NUMBER = "101";
  private static final String FIRST_NAME = "AAA";
  private static final String LAST_NAME = "BBB";
  private static final LocalDate SUBMISSION_DATE = LocalDate.now();
  private static final LocalDate DATE_ADDED = LocalDate.now();
  private static final UnderNotice UNDER_NOTICE = UnderNotice.NO;
  private static final String SANCTION = "sanction";
  private static final RecommendationStatus RECOMMENDATION_STATUS = RecommendationStatus.NOT_STARTED;
  private static final LocalDate LAST_UPDATED = LocalDate.now();
  private static final String DESIGNATED_BODY_CODE = "PQR";
  private static final String ADMIN = "Reval Admin";
  private static final boolean EXISTS_IN_GMC = true;
  private static final String OUTCOME = String.valueOf(RecommendationGmcOutcome.UNDER_REVIEW);

  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  @Mock
  private ElasticsearchIndexService elasticsearchIndexServiceMock;
  private final MasterDoctorViewMapper mapper = new MasterDoctorViewMapperImpl();
  private GmcDoctorMessageListener gmcDoctorMessageListener;

  @Captor
  ArgumentCaptor<List<MasterDoctorView>> payloadCaptor;

  @BeforeEach
  void setUp() {
    gmcDoctorMessageListener = new GmcDoctorMessageListener(doctorUpsertElasticSearchService,
        elasticsearchIndexServiceMock, mapper);

    DoctorsForDB doctor = DoctorsForDB.builder()
        .gmcReferenceNumber(GMC_NUMBER)
        .doctorFirstName(FIRST_NAME)
        .doctorLastName(LAST_NAME)
        .submissionDate(SUBMISSION_DATE)
        .dateAdded(DATE_ADDED)
        .underNotice(UNDER_NOTICE)
        .sanction(SANCTION)
        .doctorStatus(RECOMMENDATION_STATUS)
        .lastUpdatedDate(LAST_UPDATED)
        .designatedBodyCode(DESIGNATED_BODY_CODE)
        .admin(ADMIN)
        .existsInGmc(EXISTS_IN_GMC)
        .build();

    RevalidationSummaryDto summaryDto = RevalidationSummaryDto.builder()
        .doctor(doctor)
        .gmcOutcome(OUTCOME)
        .build();

    message = new IndexSyncMessage();
    message.setPayload(List.of(summaryDto));
  }

  @Test
  void testMessagesAreReceivedFromQueue() {
    gmcDoctorMessageListener.getMessage(message);

    verify(doctorUpsertElasticSearchService).populateMasterIndex(payloadCaptor.capture());
    MasterDoctorView masterDoctorView = payloadCaptor.getAllValues().get(0).get(0);

    assertThat(masterDoctorView.getGmcReferenceNumber(), is(GMC_NUMBER));
    assertThat(masterDoctorView.getDoctorFirstName(), is(FIRST_NAME));
    assertThat(masterDoctorView.getDoctorLastName(), is(LAST_NAME));
    assertThat(masterDoctorView.getSubmissionDate(), is(SUBMISSION_DATE));
    assertThat(masterDoctorView.getDesignatedBody(), is(DESIGNATED_BODY_CODE));
    assertThat(masterDoctorView.getExistsInGmc(), is(EXISTS_IN_GMC));
    assertThat(masterDoctorView.getGmcStatus(), is(OUTCOME));
  }
}
