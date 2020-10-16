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

import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.GET_TOKEN_METHOD;
import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.OIDC_ACCESS_TOKEN_HEADER;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.aggregation.AdminsAggregationStrategy;
import uk.nhs.hee.tis.revalidation.integration.router.processor.AdminsProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.GmcIdProcessorBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;

@Component
public class ProfileServiceRouter extends RouteBuilder {

  private static final String API_ADMIN_PROFILE = "/api/hee-users/${header:userName}/ignore-case?bridgeEndpoint=true";
  //private static final String API_ADMINS = "/api/hee-users?size=300";
  private static final String API_ADMINS = "/api/hee-users?bridgeEndpoint=true";

  private AdminsProcessorBean adminsProcessorBean;

  @Value("${service.profile.url}")
  private String serviceUrl;

  @Value("${service.tcs.url}")
  private String serviceUrl1;

  private KeycloakBean keycloakBean;

  ProfileServiceRouter(KeycloakBean keycloakBean, AdminsProcessorBean adminsProcessorBean) {
    this.keycloakBean = keycloakBean;
    this.adminsProcessorBean = adminsProcessorBean;
  }

  @Override
  public void configure() {
    from("direct:admin-profile")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_ADMIN_PROFILE)
        .unmarshal().json(JsonLibrary.Jackson);

    from("direct:admins")
        .process(adminsProcessorBean)
        .toD(serviceUrl + API_ADMINS)
        .unmarshal().json(JsonLibrary.Jackson);
  }
}
