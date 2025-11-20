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
  private final String gmcNumber = "101";
  private final String firstName = "AAA";
  private final String lastName = "BBB";
  private final LocalDate submissionDate = LocalDate.now();
  private final LocalDate dateAdded = LocalDate.now();
  private final UnderNotice underNotice = UnderNotice.NO;
  private final String sanction = "sanction";
  private final RecommendationStatus recommendationStatus = RecommendationStatus.NOT_STARTED;
  private final LocalDate lastUpdated = LocalDate.now();
  private final String designatedBodyCode = "PQR";
  private final String admin = "Reval Admin";
  private final boolean existsInGmc = true;
  private final String outcome = String.valueOf(RecommendationGmcOutcome.UNDER_REVIEW);

  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  @Mock
  private ElasticsearchIndexService elasticsearchIndexServiceMock;
  private final MasterDoctorViewMapper mapper = new MasterDoctorViewMapperImpl();
  private GmcDoctorMessageListener gmcDoctorMessageListener;

  @Captor
  ArgumentCaptor<RevalidationSummaryDto> dlqArgumentCaptor;
  @Captor
  ArgumentCaptor<List<MasterDoctorView>> payloadCaptor;

  @BeforeEach
  void setUp() {
    gmcDoctorMessageListener = new GmcDoctorMessageListener(doctorUpsertElasticSearchService,
        elasticsearchIndexServiceMock, mapper);

    DoctorsForDB doctor = DoctorsForDB.builder()
        .gmcReferenceNumber(gmcNumber)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .submissionDate(submissionDate)
        .dateAdded(dateAdded)
        .underNotice(underNotice)
        .sanction(sanction)
        .doctorStatus(recommendationStatus)
        .lastUpdatedDate(lastUpdated)
        .designatedBodyCode(designatedBodyCode)
        .admin(admin)
        .existsInGmc(existsInGmc)
        .build();

    RevalidationSummaryDto summaryDto = RevalidationSummaryDto.builder()
        .doctor(doctor)
        .gmcOutcome(outcome)
        .build();

    message = new IndexSyncMessage();
    message.setPayload(List.of(summaryDto));
  }

  @Test
  void testMessagesAreReceivedFromQueue() throws Exception {
    gmcDoctorMessageListener.getMessage(message);

    verify(doctorUpsertElasticSearchService).populateMasterIndex(payloadCaptor.capture());
    MasterDoctorView masterDoctorView = payloadCaptor.getAllValues().get(0).get(0);

    assertThat(masterDoctorView.getGmcReferenceNumber(), is(gmcNumber));
    assertThat(masterDoctorView.getDoctorFirstName(), is(firstName));
    assertThat(masterDoctorView.getDoctorLastName(), is(lastName));
    assertThat(masterDoctorView.getSubmissionDate(), is(submissionDate));
    assertThat(masterDoctorView.getDesignatedBody(), is(designatedBodyCode));
    assertThat(masterDoctorView.getExistsInGmc(), is(existsInGmc));
    assertThat(masterDoctorView.getGmcStatus(), is(outcome));
  }
}
