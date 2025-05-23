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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  private GmcDoctorMessageListener gmcDoctorMessageListener;
  private ObjectMapper mapper;

  @Mock
  private DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;
  @Mock
  private ElasticsearchIndexService elasticsearchIndexServiceMock;


  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    mapper.registerModules(new JavaTimeModule());
    gmcDoctorMessageListener = new GmcDoctorMessageListener(
        doctorUpsertElasticSearchService, elasticsearchIndexServiceMock, mapper);

    DoctorsForDB doctorsForDb = DoctorsForDB.builder()
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

    RevalidationSummaryDto revalidationSummaryDto = RevalidationSummaryDto.builder()
        .doctor(doctorsForDb)
        .gmcOutcome("Approved").build();

    message = new IndexSyncMessage<>();
    message.setPayload(revalidationSummaryDto);
  }

  @Test
  void testMessagesAreReceivedFromSqsQueue() throws IOException {
    String messageStr = mapper.writeValueAsString(message);
    gmcDoctorMessageListener.getMessage(messageStr);

    ArgumentCaptor<MasterDoctorView> masterDoctorViewCaptor = ArgumentCaptor
        .forClass(MasterDoctorView.class);
    verify(doctorUpsertElasticSearchService).populateMasterIndex(masterDoctorViewCaptor.capture());
    MasterDoctorView masterDoctorView = masterDoctorViewCaptor.getValue();

    assertThat(masterDoctorView.getGmcReferenceNumber(), is("101"));
    assertThat(masterDoctorView.getDoctorFirstName(), is("AAA"));
    assertThat(masterDoctorView.getDoctorLastName(), is("BBB"));
    assertThat(masterDoctorView.getSubmissionDate(), is(LocalDate.now()));
    assertThat(masterDoctorView.getDesignatedBody(), is("PQR"));
    assertThat(masterDoctorView.getExistsInGmc(), is(true));
  }

  @Test
  void shouldNotThrowErrorWhenRecommendationReindexHasException() throws Exception {
    message.setSyncEnd(true);
    String messageStr = mapper.writeValueAsString(message);

    doThrow(Exception.class).when(elasticsearchIndexServiceMock)
        .resync("masterdoctorindex", "recommendationindex");

    assertDoesNotThrow(() -> gmcDoctorMessageListener.getMessage(messageStr));
  }

  @Test
  void shouldThrowErrorWhenReadMessageFails() throws Exception {
    String messageStr = mapper.writeValueAsString(message);
    String invalidJsonStr = messageStr.substring(1, messageStr.length() - 2);

    assertThrows(JsonProcessingException.class,
        () -> gmcDoctorMessageListener.getMessage(invalidJsonStr));
  }
}
