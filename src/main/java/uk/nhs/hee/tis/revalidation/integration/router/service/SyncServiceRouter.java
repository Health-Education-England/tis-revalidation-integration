package uk.nhs.hee.tis.revalidation.integration.router.service;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.message.RabbitConfiguration;

@Component
public class SyncServiceRouter extends RouteBuilder {

  @Autowired
  RabbitConfiguration rabbitConfiguration;

  @Override
  public void configure() {
    from("direct:start-tis-sync")
      .to(rabbitConfiguration.getSyncStartRoute());
  }
}
