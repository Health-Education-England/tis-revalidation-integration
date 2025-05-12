/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date deserializer for deserializing dates for cdc messages.
 */
public class CdcDateDeserializer extends JsonDeserializer<LocalDate> {

  private static final DateTimeFormatter CDC_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final LocalDateDeserializer localDateDeserializer;

  public CdcDateDeserializer() {
    this.localDateDeserializer = new LocalDateDeserializer(DateTimeFormatter.ISO_DATE);
  }

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctx)
      throws IOException {

    try {
      JsonToken token = p.getCurrentToken();
      if (token == JsonToken.START_OBJECT) {
        JsonNode node = p.readValueAsTree();
        String dateStr = node.get("$date").asText();
        return LocalDate.parse(dateStr, ISO_DATE_TIME);
      } else {
        String dateStr = p.getText();
        return LocalDate.parse(dateStr, CDC_DATE_FORMAT);
      }
    } catch (DateTimeParseException e) {
      return localDateDeserializer.deserialize(p, ctx);
    }
  }
}
