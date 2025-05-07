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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import javax.naming.OperationNotSupportedException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.CdcDocumentDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcDoctorService;
import uk.nhs.hee.tis.revalidation.integration.entity.DoctorsForDB;

@ExtendWith(MockitoExtension.class)
class CdcDoctorMessageHandlerTest {

  @InjectMocks
  CdcDoctorMessageHandler cdcDoctorMessageHandler;

  @Mock
  CdcDoctorService cdcDoctorService;

  @Test
  void shouldRejectOtherDoctorOperationMessageFromSqsQueueToHandler() {
    var testMessage = CdcTestDataGenerator.getCdcDoctorUnsupportedCdcDocumentDto();
    assertThrows(OperationNotSupportedException.class,
        () -> cdcDoctorMessageHandler.handleMessage(testMessage));
  }

  private static List<CdcDocumentDto<DoctorsForDB>> cdcDtos() {
    return Arrays.asList(
        CdcTestDataGenerator.getCdcDoctorUpdateCdcDocumentDto(),
        CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto(),
        CdcTestDataGenerator.getCdcDoctorReplaceCdcDocumentDto());
  }

  @ParameterizedTest
  @MethodSource("cdcDtos")
  void shouldHandleDifferentOperationTypes() throws OperationNotSupportedException {
    var testMessage = CdcTestDataGenerator.getCdcDoctorInsertCdcDocumentDto();
    cdcDoctorMessageHandler.handleMessage(testMessage);

    verify(cdcDoctorService).upsertEntity(testMessage.getFullDocument());
  }
}
