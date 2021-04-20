package uk.nhs.hee.tis.revalidation.integration.router.message;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConnectionMessageRouter extends RouteBuilder {

  RabbitConfiguration rabbitConfiguration;

  private final String revalSyncDataRoute;
  private final String revalConnectionUpdateRoute;
  private final String revalSyncStartRoute;

  public ConnectionMessageRouter(RabbitConfiguration rabbitConfiguration) {
    super();
    this.rabbitConfiguration = rabbitConfiguration;
    this.revalSyncStartRoute = rabbitConfiguration.getSyncStartRoute();
    this.revalSyncDataRoute = rabbitConfiguration.getSyncDataRoute();
    this.revalConnectionUpdateRoute =  rabbitConfiguration.getConnectionUpdateRoute();
  }

  @Override
  public void configure() throws Exception {
    from(revalSyncDataRoute).id("updateQueue")
        .log("Message received: ${body}");
        //TODO Remove log and pipe to elasticsearch
  }
}
