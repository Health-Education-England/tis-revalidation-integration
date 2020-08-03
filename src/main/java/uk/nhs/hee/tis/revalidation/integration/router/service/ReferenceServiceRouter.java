package uk.nhs.hee.tis.revalidation.integration.router.service;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReferenceServiceRouter extends RouteBuilder {

  private static final String API_SITES = "/api/sites?bridgeEndpoint=true";

  @Value("${service.reference.url}")
  private String serviceUrl;

  @Override
  public void configure() throws Exception {
    from("direct:reference-sites")
        .to(serviceUrl + API_SITES)
        .unmarshal().json(JsonLibrary.Jackson);
  }
}
