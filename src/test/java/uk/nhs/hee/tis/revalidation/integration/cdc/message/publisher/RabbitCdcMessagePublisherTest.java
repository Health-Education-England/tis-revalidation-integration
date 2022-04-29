package uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher;

import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.rabbit.RabbitCdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@ExtendWith(MockitoExtension.class)
class RabbitCdcMessagePublisherTest {
  @InjectMocks
  RabbitCdcMessagePublisher rabbitCdcMessagePublisher;

  @Mock
  RabbitTemplate rabbitTemplate;

  private MasterDoctorView masterDoctorView;
  private String gmcReferenceNumber = "1234567";
  private String connectionKey = "reval.connection.update";
  private String recommendationKey = "reval.masterdoctorview.updated";

  @BeforeEach
  void setup() {
    masterDoctorView = MasterDoctorView.builder()
        .gmcReferenceNumber(gmcReferenceNumber)
        .build();
    setField(rabbitCdcMessagePublisher, "connectionUpdateKey", connectionKey);
    setField(rabbitCdcMessagePublisher, "recommendationUpdateKey", recommendationKey);
  }

  @Test
  void shouldPublishConnectionUpdatesUsingRabbitTemplate() {
    rabbitCdcMessagePublisher.publishCdcConnectionUpdate(masterDoctorView);

    verify(rabbitTemplate).convertAndSend(connectionKey, masterDoctorView);
  }

  @Test
  void shouldPublishRecommendationUpdatesUsingRabbitTemplate() {
    rabbitCdcMessagePublisher.publishCdcRecommendationUpdate(masterDoctorView);

    verify(rabbitTemplate).convertAndSend(recommendationKey, masterDoctorView);
  }
}
