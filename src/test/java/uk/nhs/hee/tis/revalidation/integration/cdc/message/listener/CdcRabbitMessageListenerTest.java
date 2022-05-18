package uk.nhs.hee.tis.revalidation.integration.cdc.message.listener;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.integration.cdc.dto.TraineeUpdateDto;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.handler.CdcTraineeUpdateMessageHandler;

@ExtendWith(MockitoExtension.class)
class CdcRabbitMessageListenerTest {

  @InjectMocks
  CdcRabbitMessageListener cdcRabbitMessageListener;

  @Mock
  CdcTraineeUpdateMessageHandler cdcTraineeUpdateHandler;

  private TraineeUpdateDto traineeUpdateDto;

  @Test
  void shouldReceiveTraineeUpdates() {
    traineeUpdateDto = TraineeUpdateDto.builder().build();
    cdcRabbitMessageListener.getTraineeUpdateMessage(traineeUpdateDto);
    verify(cdcTraineeUpdateHandler).handleMessage(traineeUpdateDto);
  }
}
