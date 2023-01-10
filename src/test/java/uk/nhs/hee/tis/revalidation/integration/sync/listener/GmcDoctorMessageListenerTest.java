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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import io.awspring.cloud.messaging.listener.Visibility;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.integration.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.integration.router.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.integration.router.message.payload.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;
import uk.nhs.hee.tis.revalidation.integration.sync.service.ElasticsearchIndexService;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class GmcDoctorMessageListenerTest {

  private IndexSyncMessage<RevalidationSummaryDto> message;
  private RevalidationSummaryDto revalidationSummaryDto;
  private DoctorsForDB doctorsForDB;

  @InjectMocks
  private GmcDoctorMessageListener gmcDoctorMessageListener;
  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  @Mock
  private ElasticsearchIndexService elasticsearchIndexServiceMock;

  @Mock
  private Visibility visibilityMock;

  @BeforeEach
  void setUp() {
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
        .admin("Reval Admin")
        .existsInGmc(true).build();

    revalidationSummaryDto = RevalidationSummaryDto.builder()
        .doctor(doctorsForDB)
        .gmcOutcome("Approved").build();

    message = new IndexSyncMessage<RevalidationSummaryDto>();
    message.setPayload(revalidationSummaryDto);
  }

  @Mock
  private Future<ChangeMessageVisibilityResult> futureMock;

  @Test
  void testMessagesAreReceivedFromSqsQueue() throws Exception {
    gmcDoctorMessageListener.getMessage(message, visibilityMock);

    ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor = ArgumentCaptor
        .forClass(MasterDoctorView.class);
    verify(doctorUpsertElasticSearchService).populateMasterIndex(masterDoctorViewCaptor.capture());
    MasterDoctorView masterDoctorView = masterDoctorViewCaptor.getValue();

    assertThat(masterDoctorView.getGmcReferenceNumber(), is("101"));
    assertThat(masterDoctorView.getDoctorFirstName(), is("AAA"));
    assertThat(masterDoctorView.getDoctorLastName(), is("BBB"));
    assertThat(masterDoctorView.getSubmissionDate(), is(LocalDate.now()));
    assertThat(masterDoctorView.getDesignatedBody(), is("PQR"));
    assertThat(masterDoctorView.getConnectionStatus(), is("Yes"));
    assertThat(masterDoctorView.getExistsInGmc(), is(true));
  }

  @Test
  void shouldNotThrowErrorWhenRecommendationReindexHasException() throws Exception {
    message.setSyncEnd(true);

    doReturn(futureMock).when(visibilityMock).extend(anyInt());

    doThrow(Exception.class).when(elasticsearchIndexServiceMock)
        .resync("masterdoctorindex", "recommendationindex");

    assertDoesNotThrow(() -> gmcDoctorMessageListener.getMessage(message, visibilityMock));
  }

  @Test
  void shouldNotResyncWhenExtendVisibilityThrowsExecutionException() throws Exception {
    message.setSyncEnd(true);

    doReturn(futureMock).when(visibilityMock).extend(anyInt());
    doThrow(ExecutionException.class).when(futureMock).get();

    assertDoesNotThrow(() -> gmcDoctorMessageListener.getMessage(message, visibilityMock));
    verify(elasticsearchIndexServiceMock, never()).resync(anyString(), anyString());
  }

  @Test
  void shouldNotResyncWhenExtendVisibilityThrowsInterruptedException() throws Exception {
    message.setSyncEnd(true);
    InterruptedException expectedException = new InterruptedException("expected");

    doReturn(futureMock).when(visibilityMock).extend(anyInt());
    doThrow(expectedException).when(futureMock).get();

    var actual = assertThrows(InterruptedException.class,
        () -> gmcDoctorMessageListener.getMessage(message, visibilityMock));
    verify(elasticsearchIndexServiceMock, never()).resync(anyString(), anyString());
    assertEquals(expectedException, actual);
  }
}
