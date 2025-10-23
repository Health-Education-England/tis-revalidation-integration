package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date deserializer for deserializing local datetimes for cdc messages.
 */
public class CdcLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

  private static final DateTimeFormatter CDC_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final LocalDateTimeDeserializer localDateTimeDeserializer;

  public CdcLocalDateTimeDeserializer() {
    this.localDateTimeDeserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE);
  }

  @Override
  public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx)
      throws IOException {

    try {
      JsonToken token = p.getCurrentToken();
      if (token == JsonToken.START_OBJECT) {
        JsonNode node = p.readValueAsTree();
        String dateStr = node.get("$date").asText();
        return LocalDateTime.parse(dateStr, ISO_DATE_TIME);
      } else {
        String dateStr = p.getText();
        return LocalDateTime.parse(dateStr, CDC_DATE_FORMAT);
      }
    } catch (DateTimeParseException e) {
      return localDateTimeDeserializer.deserialize(p, ctx);
    }
  }
}
