package uk.nhs.hee.tis.revalidation.integration.router.message;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Handler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.sync.service.DoctorUpsertElasticSearchService;

@Slf4j
@Component
public class SyncStartHandler {

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  @Value("${app.rabbit.reval.routingKey.connection.syncstart}")
  private String revalSyncStartRoutingKey;

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Autowired
  DoctorUpsertElasticSearchService doctorUpsertElasticSearchService;

  @Handler
  public void startTraineeSync() {
    doctorUpsertElasticSearchService.clearMasterDoctorIndex();
    rabbitTemplate.convertAndSend(revalExchange, revalSyncStartRoutingKey, "syncStart");
  }
}
