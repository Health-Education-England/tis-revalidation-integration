package uk.nhs.hee.tis.revalidation.integration.router.message;

import org.apache.camel.Handler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SyncStartHandler {

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  @Value("${app.rabbit.reval.routingKey.connection.syncstart}")
  private String revalSyncStartRoutingKey;

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Handler
  public void startTraineeSync() {
    rabbitTemplate.convertAndSend(revalExchange, revalSyncStartRoutingKey, "syncStart");
  }

}
