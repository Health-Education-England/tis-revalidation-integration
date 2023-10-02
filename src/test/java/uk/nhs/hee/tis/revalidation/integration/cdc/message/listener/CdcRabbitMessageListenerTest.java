package uk.nhs.hee.tis.revalidation.integration.cdc.message.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.ConnectionInfoDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcTraineeUpdateMessageHandler;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.testutil.CdcTestDataGenerator;

@ExtendWith(MockitoExtension.class)
class CdcRabbitMessageListenerTest {

  @InjectMocks
  CdcRabbitMessageListener cdcRabbitMessageListener;

  @Mock
  CdcTraineeUpdateMessageHandler cdcTraineeUpdateHandler;

  private ConnectionInfoDto connectionInfoDto;

  @Test
  void shouldReceiveTraineeUpdates() {
    connectionInfoDto = CdcTestDataGenerator.getConnectionInfo();
    cdcRabbitMessageListener.getTraineeUpdateMessage(connectionInfoDto);
    verify(cdcTraineeUpdateHandler).handleMessage(connectionInfoDto);
  }

  @Test
  void shouldExceptionWhenTisPersonIdIsNull() {
    connectionInfoDto = ConnectionInfoDto.builder().build();
    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        cdcRabbitMessageListener.getTraineeUpdateMessage(connectionInfoDto));

    String expectedMessage = "Received update message from TIS with null tis personId";
    assertTrue(exception.getMessage().contains(expectedMessage));
  }
}
