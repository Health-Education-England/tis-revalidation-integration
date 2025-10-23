package uk.nhs.hee.tis.revalidation.integration.cdc.message.handler;

import static org.mockito.Mockito.verify;

import javax.naming.OperationNotSupportedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;
import uk.nhs.hee.tis.revalidation.integration.cdc.service.CdcConnectionService;

@ExtendWith(MockitoExtension.class)
class CdcConnectionMessageHandlerTest {

  @InjectMocks
  CdcConnectionMessageHandler cdcConnectionMessageHandler;

  @Mock
  CdcConnectionService cdcConnectionService;

  @Test
  void shouldHandleInserts() throws OperationNotSupportedException {
    var testMessage = CdcTestDataGenerator.getCdcConnectionLogInsertCdcDocumentDto();
    cdcConnectionMessageHandler.handleMessage(testMessage);

    verify(cdcConnectionService).upsertEntity(testMessage.getFullDocument());
  }
}
