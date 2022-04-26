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

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.naming.OperationNotSupportedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcDoctorMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcRecommendationMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.util.CdcDateDeserializer;

@ExtendWith(MockitoExtension.class)
class CdcSqsMessageListenerTest {

  @InjectMocks
  CdcSqsMessageListener cdcSqsMessageListener;

  @Mock
  CdcRecommendationMessageHandler cdcRecommendationMessageHandler;

  @Mock
  CdcDoctorMessageHandler cdcDoctorMessageHandler;

  @Spy
  ObjectMapper objectMapper;

  @BeforeEach
  void initTestClass() {
    SimpleModule customDeserializationModule = new SimpleModule();
    customDeserializationModule.addDeserializer(LocalDate.class, new CdcDateDeserializer());
    //TODO: Remove the serializer here or `@JsonSerialize` on entity attributes
    customDeserializationModule.addSerializer(LocalDate.class, new LocalDateSerializer(
        DateTimeFormatter.ISO_DATE));
    objectMapper.registerModule(customDeserializationModule);
  }

  @Test
  void shouldPassDoctorInsertMessageFromSqsQueueToHandler()
      throws OperationNotSupportedException, IOException {
    var testMessage = objectMapper.writeValueAsString(
        CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto()
    );
    cdcSqsMessageListener.getDoctorMessage(testMessage);

    verify(cdcDoctorMessageHandler).handleMessage(
        objectMapper.readValue(testMessage, new TypeReference<>() {
        })
    );
  }

  @Test
  void shouldPassRecommendationInsertMessageFromSqsQueueToHandler()
      throws OperationNotSupportedException, IOException {
    var testMessage = objectMapper.writeValueAsString(
        CdcTestDataGenerator.getCdcRecommendationInsertCdcDocumentDto()
    );
    cdcSqsMessageListener.getRecommendationMessage(testMessage);

    verify(cdcRecommendationMessageHandler).handleMessage(
        objectMapper.readValue(testMessage, new TypeReference<>() {
        })
    );
  }
}
