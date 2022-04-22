package uk.nhs.hee.tis.revalidation.integration.cdc.message.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CdcDateDeserializer extends JsonDeserializer<LocalDate> {

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctx)
      throws IOException, JsonProcessingException {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String dateString = p.getText();
    try {
      Date date = format.parse(dateString);
      return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    } catch (ParseException e) {
      LocalDateDeserializer lds = new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE);
      return lds.deserialize(p,ctx);
    }
  }
}
