package uk.nhs.hee.tis.revalidation.integration.router.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class CommonUploadApiRouter extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    restConfiguration().component("servlet").bindingMode(RestBindingMode.auto);

    rest("/storage/upload")
        .post().to("direct:storage-upload");

    rest("/storage/download")
        .get().to("direct:storage-download");

    rest("/storage/list")
        .get().to("direct:storage-list");

    rest("/storage/delete")
        .delete().to("direct:storage-delete");
  }
}
