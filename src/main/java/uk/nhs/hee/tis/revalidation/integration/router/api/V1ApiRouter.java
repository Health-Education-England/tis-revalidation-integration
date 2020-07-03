package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class V1ApiRouter extends RouteBuilder {

  @Override
  public void configure() {
    restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);

    rest("/v1/admin")
        .post().to("direct:admin");

    rest("/v1/doctors")
        .get().to("direct:doctors");
  }
}

