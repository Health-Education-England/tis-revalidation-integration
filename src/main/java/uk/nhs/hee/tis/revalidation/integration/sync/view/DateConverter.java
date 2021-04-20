package uk.nhs.hee.tis.revalidation.integration.sync.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
@Component
public class DateConverter implements Converter<Long, LocalDate> {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  @Override
  public LocalDate convert(Long source) {
    try {
      return getDateFromTimestamp(source);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  public static LocalDateTime getDateTimeFromTimestamp(long timestamp) {
    if (timestamp == 0)
      return null;
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), TimeZone
        .getDefault().toZoneId());
  }

  public static LocalDate getDateFromTimestamp(long timestamp) {
    LocalDateTime date = getDateTimeFromTimestamp(timestamp);
    return date == null ? null : date.toLocalDate();
  }
}
