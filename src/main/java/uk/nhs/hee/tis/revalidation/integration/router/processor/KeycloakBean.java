package uk.nhs.hee.tis.revalidation.integration.router.processor;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeycloakBean {

  @Autowired
  private Keycloak keycloak;

  public String getAuthToken() {
    final var token = "Bearer " + keycloak.tokenManager().getAccessTokenString();
    log.info("Token: {}", token);

    return token;
  }
}
