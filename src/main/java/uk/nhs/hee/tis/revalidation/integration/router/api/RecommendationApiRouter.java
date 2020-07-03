package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class RecommendationApiRouter extends RouteBuilder {

  @Override
  public void configure() {
    restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);

    rest("/recommendation")
        .post().to("direct:recommendation-post");

    rest("/recommendation")
        .put().to("direct:recommendation-put");

    rest("/recommendation")
        .get("/{gmcId}").to("direct:recommendation-gmc-id");

    rest("/recommendation")
        .post("/{gmcId}/submit/{recommendationId}").to("direct:recommendation-submit");
  }
}
