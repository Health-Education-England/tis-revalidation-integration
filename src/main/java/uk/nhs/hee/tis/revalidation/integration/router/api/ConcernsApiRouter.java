package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class ConcernsApiRouter extends RouteBuilder {

  @Override
  public void configure() {
    restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);

    rest("/concerns")
        .get().to("direct:concerns");
  }
}
