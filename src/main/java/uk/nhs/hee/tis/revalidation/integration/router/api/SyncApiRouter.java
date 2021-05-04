package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class SyncApiRouter extends RouteBuilder {
  @Override
  public void configure() {
    restConfiguration().component("servlet");

    rest("/sync")
        .get().bindingMode(RestBindingMode.auto).to("direct:start-tis-sync");
  }
}
