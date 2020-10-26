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

import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.GET_ROLE_NAMES_HEADER;
import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.GET_ROLE_NAMES_METHOD;
import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.GET_TOKEN_METHOD;
import static uk.nhs.hee.tis.revalidation.integration.router.helper.Constants.OIDC_ACCESS_TOKEN_HEADER;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.integration.router.processor.KeycloakBean;
import uk.nhs.hee.tis.revalidation.integration.router.processor.RoleNameBean;

@Component
public class ProfileServiceRouter extends RouteBuilder {

  private static final String API_ADMIN_PROFILE = "/api/hee-users/${header:userName}/ignore-case?bridgeEndpoint=true";
  private static final String API_ASSIGN_REVAL_ADMINS = "/api/hee-users-with-roles/${header:roleNames}?bridgeEndpoint=true";

  @Value("${service.profile.url}")
  private String serviceUrl;

  private KeycloakBean keycloakBean;

  private RoleNameBean roleNameBean;

  ProfileServiceRouter(KeycloakBean keycloakBean,
      RoleNameBean roleNameBean) {
    this.keycloakBean = keycloakBean;
    this.roleNameBean = roleNameBean;
  }

  @Override
  public void configure() {
    from("direct:admin-profile")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .toD(serviceUrl + API_ADMIN_PROFILE);

    from("direct:admins")
        .setHeader(OIDC_ACCESS_TOKEN_HEADER).method(keycloakBean, GET_TOKEN_METHOD)
        .setHeader(GET_ROLE_NAMES_HEADER).method(roleNameBean, GET_ROLE_NAMES_METHOD)
        .toD(serviceUrl + API_ASSIGN_REVAL_ADMINS);
  }
}
