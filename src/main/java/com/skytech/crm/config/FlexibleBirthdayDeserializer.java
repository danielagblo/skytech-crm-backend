package com.skytech.crm.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

/** Accepts legacy ISO dates and yearless birthdays in MM-dd format. */
public class FlexibleBirthdayDeserializer extends JsonDeserializer<LocalDate> {
  private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");
  private static final int YEARLESS_BIRTHDAY_STORAGE_YEAR = 2000;

  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String value = parser.getValueAsString();
    if (value == null || value.isBlank()) return null;
    try {
      String normalized = value.trim();
      if (normalized.matches("\\d{2}-\\d{2}"))
        return MonthDay.parse(normalized, MONTH_DAY).atYear(YEARLESS_BIRTHDAY_STORAGE_YEAR);
      return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (DateTimeException exception) {
      throw InvalidFormatException.from(
          parser,
          "Birthday must be a valid MM-DD or YYYY-MM-DD value",
          value,
          LocalDate.class);
    }
  }
}
