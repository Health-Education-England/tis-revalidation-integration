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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_CONNECTION_LOG_EVENT_JSON;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_DOC_JSON;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_HIDDEN_DISCREPANCY_DELETE_EVENT;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_HIDDEN_DISCREPANCY_INSERT_EVENT;
import static uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator.CDC_RECOMMENDATION_EVENT_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.naming.OperationNotSupportedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcConnectionMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcDoctorMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcHiddenDiscrepancyMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcRecommendationMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.entity.ConnectionLog;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.integration.entity.HiddenDiscrepancy;
import uk.nhs.hee.tis.revalidation.integration.entity.Recommendation;

@ExtendWith(MockitoExtension.class)
class CdcSqsMessageListenerTest {

  private static final String GMC_ID = "1234567";
  private static final String HIDDEN_DISCREPANCY_OID = "69fdb35117c18114b019a064";

  @InjectMocks
  CdcSqsMessageListener cdcSqsMessageListener;

  @Mock
  CdcRecommendationMessageHandler cdcRecommendationMessageHandler;

  @Mock
  CdcDoctorMessageHandler cdcDoctorMessageHandler;

  @Mock
  CdcConnectionMessageHandler cdcConnectionMessageHandler;

  @Mock
  CdcHiddenDiscrepancyMessageHandler cdcHiddenDiscrepancyMessageHandler;

  @Spy
  ObjectMapper objectMapper;

  @Captor
  ArgumentCaptor<String> messageCaptor;

  @Captor
  ArgumentCaptor<CdcDocumentDto<DoctorsForDB>> doctorMessageCaptor;

  @Captor
  ArgumentCaptor<CdcDocumentDto<Recommendation>> recommendationMessageCaptor;

  @Captor
  ArgumentCaptor<CdcDocumentDto<ConnectionLog>> connectionMessageCaptor;

  @Captor
  ArgumentCaptor<CdcDocumentDto<HiddenDiscrepancy>> hiddenDiscrepancyMessageCaptor;

  @Test
  void shouldPassDoctorMessageFromSqsQueueToHandler()
      throws OperationNotSupportedException, IOException {

    cdcSqsMessageListener.getDoctorMessage(CDC_DOC_JSON);

    verify(cdcDoctorMessageHandler).handleMessage(doctorMessageCaptor.capture());

    var result = doctorMessageCaptor.getValue();
    assertEquals(GMC_ID, result.getFullDocument().getGmcReferenceNumber());
    assertEquals("replace", result.getOperationType());
  }

  @Test
  void shouldPassRecommendationInsertMessageFromSqsQueueToHandler()
      throws OperationNotSupportedException, IOException {

    cdcSqsMessageListener.getRecommendationMessage(CDC_RECOMMENDATION_EVENT_JSON);

    verify(cdcRecommendationMessageHandler).handleMessage(recommendationMessageCaptor.capture());

    var result = recommendationMessageCaptor.getValue();
    assertEquals(GMC_ID, result.getFullDocument().getGmcNumber());
    assertEquals("update", result.getOperationType());
  }

  @Test
  void shouldPassConnectionLogMessagesFromSqsQueueToHandler()
      throws OperationNotSupportedException, IOException {

    cdcSqsMessageListener.getConnectionMessage(CDC_CONNECTION_LOG_EVENT_JSON);

    verify(cdcConnectionMessageHandler).handleMessage(connectionMessageCaptor.capture());

    var result = connectionMessageCaptor.getValue();
    assertEquals(GMC_ID, result.getFullDocument().getGmcId());
    assertEquals("insert", result.getOperationType());
  }

  @Test
  void shouldPassHiddenDiscrepancyInsertMessageFromSqsQueueToHandlerWithObjectId()
      throws OperationNotSupportedException, IOException {

    cdcSqsMessageListener.getHiddenDiscrepancyMessage(CDC_HIDDEN_DISCREPANCY_INSERT_EVENT);

    verify(cdcHiddenDiscrepancyMessageHandler)
        .handleMessage(hiddenDiscrepancyMessageCaptor.capture());

    var result = hiddenDiscrepancyMessageCaptor.getValue();
    assertEquals(GMC_ID, result.getFullDocument().getGmcId());
    assertEquals(HIDDEN_DISCREPANCY_OID, result.getFullDocument().getId());
    assertEquals("insert", result.getOperationType());
  }

  @Test
  void shouldPassHiddenDiscrepancyDeleteMessageFromSqsQueueToHandlerWithObjectId()
      throws OperationNotSupportedException, IOException {
    cdcSqsMessageListener.getHiddenDiscrepancyMessage(CDC_HIDDEN_DISCREPANCY_DELETE_EVENT);

    verify(cdcHiddenDiscrepancyMessageHandler)
        .handleMessage(hiddenDiscrepancyMessageCaptor.capture());

    var result = hiddenDiscrepancyMessageCaptor.getValue();
    assertEquals(HIDDEN_DISCREPANCY_OID, result.getTargetObjectId());
    assertEquals("delete", result.getOperationType());
  }
}
