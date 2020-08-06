package uk.nhs.hee.tis.revalidation.integration.router.processor;

import com.transformuk.hee.tis.reference.client.ReferenceService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReferenceBean {

//  private static final String API_SITES = "http://stage-apps.tis.nhs.uk/reference/api/sites";
//
//  @Value("${service.reference.url}")
//  private String serviceUrl;
//


  @Autowired
  //private ReferenceService referenceService;
  //private RestTemplate restTemplate;
  private Keycloak keycloak;

  public String doSomething() {
    final var token = "Bearer " + keycloak.tokenManager().getAccessTokenString();
    System.out.println("Token:" + token);

    return token;
    //final ResponseEntity<String> data = restTemplate.getForEntity(API_SITES, String.class);
//    final var allLocalOffice = referenceService.findAllLocalOffice();
//    System.out.println("ReferenceData: " + allLocalOffice);
  }
}
