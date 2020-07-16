package uk.nhs.hee.tis.revalidation.integration.router.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class CommonUploadServiceRouter extends RouteBuilder {

  private static final String API_STORAGE_UPLOAD = "/api/storage/upload?bridgeEndpoint=true";
  private static final String API_STORAGE_DOWNLOAD = "/api/storage/download?bridgeEndpoint=true";
  private static final String API_STORAGE_LIST = "/api/storage/list?bridgeEndpoint=true";

  @Value("${service.common-upload.url}")
  private String serviceUrl;

  @Override
  public void configure() throws Exception {

    from("direct:storage-upload")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .toD(serviceUrl + API_STORAGE_UPLOAD);

    from("direct:storage-download")
        .to(serviceUrl + API_STORAGE_DOWNLOAD);

    from("direct:storage-list")
        .to(serviceUrl + API_STORAGE_LIST);

  }
}
