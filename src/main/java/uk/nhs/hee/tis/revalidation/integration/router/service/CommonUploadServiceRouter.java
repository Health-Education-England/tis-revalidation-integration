/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.integration.router.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class CommonUploadServiceRouter extends RouteBuilder {

  private static final String API_STORAGE_UPLOAD = "/api/storage/upload?bridgeEndpoint=true";
  private static final String API_STORAGE_DOWNLOAD = "/api/storage/download?bridgeEndpoint=true";
  private static final String API_STORAGE_LIST = "/api/storage/list?bridgeEndpoint=true";
  private static final String API_STORAGE_DELETE = "/api/storage/delete?bridgeEndpoint=true";

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
        .to(serviceUrl + API_STORAGE_LIST)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:storage-delete")
        .to(serviceUrl + API_STORAGE_DELETE);
  }
}
