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

package uk.nhs.hee.tis.revalidation.integration;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transformuk.hee.tis.security.client.KeycloakClientRequestFactory;
import com.transformuk.hee.tis.security.client.KeycloakRestTemplate;
import java.util.HashMap;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

@ComponentScan(basePackages = {"uk.nhs.hee.tis.revalidation.integration",
    "com.transformuk.hee.tis.reference"})
@SpringBootApplication
public class RevalidationIntegrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(RevalidationIntegrationApplication.class);
  }

  @Bean
  @Primary
  public ObjectMapper mapper() {
    final var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(ALLOW_UNQUOTED_FIELD_NAMES, true);
    return mapper;
  }

  @Bean(name = "json-jackson")
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public JacksonDataFormat jacksonDataFormat(ObjectMapper mapper) {
    return new JacksonDataFormat(mapper, HashMap.class);
  }


  @Bean
  public RestTemplate restTemplate(final Keycloak keycloak) {
    final var keycloakClientRequestFactory = new KeycloakClientRequestFactory(keycloak);
    return new KeycloakRestTemplate(keycloakClientRequestFactory);
  }
}
