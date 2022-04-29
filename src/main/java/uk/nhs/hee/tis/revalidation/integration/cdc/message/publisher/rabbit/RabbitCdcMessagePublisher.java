package uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.cdc.message.publisher.CdcMessagePublisher;
import uk.nhs.hee.tis.revalidation.integration.sync.view.MasterDoctorView;

@Component
public class RabbitCdcMessagePublisher implements CdcMessagePublisher {

  private RabbitTemplate rabbitTemplate;

  @Value("${app.rabbit.reval.routingKey.connection.update}")
  private String connectionUpdateKey;

  @Value("${app.rabbit.reval.routingKey.masterdoctorview.updated}")
  private String recommendationUpdateKey;

  public RabbitCdcMessagePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publish MasterDoctorView update to Connections Service using rabbit template
   *
   * @param update the updated MasterDoctorView to be published
   */
  @Override
  public void publishCdcConnectionUpdate(
      MasterDoctorView update) {
    rabbitTemplate.convertAndSend(connectionUpdateKey, update);
  }

  /**
   * Publish MasterDoctorView update to Recommendations Service using rabbit template
   *
   * @param update the updated MasterDoctorView to be published
   */
  @Override
  public void publishCdcRecommendationUpdate(
      MasterDoctorView update) {
    rabbitTemplate.convertAndSend(recommendationUpdateKey, update);
  }
}
