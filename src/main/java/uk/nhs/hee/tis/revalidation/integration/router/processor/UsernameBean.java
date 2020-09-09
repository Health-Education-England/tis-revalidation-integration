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

package uk.nhs.hee.tis.revalidation.integration.router.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.apache.camel.Header;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class UsernameBean {

  // TODO: replace with non-custom field when userpool recreated.
  private static final String USERNAME_KEY = "custom:preferred_username";

  /**
   * Get the username from the claims in the authorization token.
   *
   * @param token The authorization token from the headers.
   * @return The preferred username.
   * @throws IOException If the claims could not be read.
   */
  public String getUsername(@Header(HttpHeaders.AUTHORIZATION) String token) throws IOException {
    String[] tokens = token.split("\\.");
    byte[] claimsBytes = Base64.getDecoder().decode(tokens[1].getBytes(StandardCharsets.UTF_8));

    ObjectMapper mapper = new ObjectMapper();
    Map claims = mapper.readValue(claimsBytes, Map.class);
    return (String) claims.get(USERNAME_KEY);
  }
}
