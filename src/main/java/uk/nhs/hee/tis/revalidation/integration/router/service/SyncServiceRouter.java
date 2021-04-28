package uk.nhs.hee.tis.revalidation.integration.router.service;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SyncServiceRouter extends RouteBuilder {

  @Override
  public void configure() {
    from("direct:start-tis-sync")
    .to("bean:syncStartHandler");
  }
}
