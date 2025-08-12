package com.agilecheckup.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateTimeUtilTest {

  @Test
  void shouldParseISO8601DateTimeWithMillisecondsAndTimezone() {
    // Given
    String iso8601WithMillisAndTz = "2025-06-19T17:54:27.862Z";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(iso8601WithMillisAndTz);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(862_000_000); // 862 milliseconds
  }

  @Test
  void shouldParseISO8601DateTimeWithoutMilliseconds() {
    // Given
    String iso8601WithoutMillis = "2025-06-19T17:54:27Z";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(iso8601WithoutMillis);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(0);
  }

  @Test
  void shouldParseISO8601DateTimeWithPositiveTimezone() {
    // Given
    String iso8601WithPositiveTz = "2025-06-19T17:54:27.862+02:00";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(iso8601WithPositiveTz);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(862_000_000);
  }

  @Test
  void shouldParseISO8601DateTimeWithNegativeTimezone() {
    // Given
    String iso8601WithNegativeTz = "2025-06-19T17:54:27.862-05:00";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(iso8601WithNegativeTz);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(862_000_000);
  }

  @Test
  void shouldParseSimpleLocalDateTimeFormat() {
    // Given
    String simpleLocalDateTime = "2025-06-19T17:54:27";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(simpleLocalDateTime);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(0);
  }

  @Test
  void shouldParseLocalDateTimeWithNanoseconds() {
    // Given
    String localDateTimeWithNanos = "2025-06-19T17:54:27.123456789";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(localDateTimeWithNanos);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(6);
    assertThat(result.getDayOfMonth()).isEqualTo(19);
    assertThat(result.getHour()).isEqualTo(17);
    assertThat(result.getMinute()).isEqualTo(54);
    assertThat(result.getSecond()).isEqualTo(27);
    assertThat(result.getNano()).isEqualTo(123_456_789);
  }

  @Test
  void shouldThrowExceptionForNullInput() {
    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDateTime(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("DateTime string cannot be null");
  }

  @Test
  void shouldThrowExceptionForEmptyString() {
    // Given
    String emptyString = "";

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDateTime(emptyString)).isInstanceOf(DateTimeParseException.class).hasMessageContaining("Unable to parse datetime: " + emptyString);
  }

  @Test
  void shouldThrowExceptionForInvalidDateFormat() {
    // Given
    String invalidDate = "2025/06/19 17:54:27";

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDateTime(invalidDate)).isInstanceOf(DateTimeParseException.class).hasMessageContaining("Unable to parse datetime: " + invalidDate).hasMessageContaining("Expected ISO8601 format");
  }

  @Test
  void shouldThrowExceptionForInvalidISO8601Format() {
    // Given
    String invalidISO8601 = "2025-06-19T25:54:27Z"; // Invalid hour

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDateTime(invalidISO8601)).isInstanceOf(DateTimeParseException.class).hasMessageContaining("Unable to parse datetime: " + invalidISO8601);
  }

  @Test
  void shouldThrowExceptionForMalformedTimezone() {
    // Given
    String malformedTimezone = "2025-06-19T17:54:27+25:00"; // Invalid timezone offset

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDateTime(malformedTimezone)).isInstanceOf(DateTimeParseException.class).hasMessageContaining("Unable to parse datetime: " + malformedTimezone);
  }

  @Test
  void shouldHandleLeapYear() {
    // Given
    String leapYearDate = "2024-02-29T12:00:00Z";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(leapYearDate);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2024);
    assertThat(result.getMonthValue()).isEqualTo(2);
    assertThat(result.getDayOfMonth()).isEqualTo(29);
  }

  @Test
  void shouldHandleEndOfMonth() {
    // Given
    String endOfMonth = "2025-01-31T23:59:59.999Z";

    // When
    LocalDateTime result = DateTimeUtil.parseDateTime(endOfMonth);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2025);
    assertThat(result.getMonthValue()).isEqualTo(1);
    assertThat(result.getDayOfMonth()).isEqualTo(31);
    assertThat(result.getHour()).isEqualTo(23);
    assertThat(result.getMinute()).isEqualTo(59);
    assertThat(result.getSecond()).isEqualTo(59);
    assertThat(result.getNano()).isEqualTo(999_000_000);
  }

  // Tests for parseDate method

  @Test
  void shouldParseDateFromISO8601DateTime() {
    // Given
    String iso8601DateTime = "2025-06-19T17:54:27.862Z";

    // When
    Date result = DateTimeUtil.parseDate(iso8601DateTime);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  void shouldParseDateFromSimpleDateFormat() {
    // Given
    String simpleDate = "2025-06-19";

    // When
    Date result = DateTimeUtil.parseDate(simpleDate);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  void shouldParseDateFromTimestamp() {
    // Given
    Long timestamp = 1718813067862L; // Some timestamp

    // When
    Date result = DateTimeUtil.parseDate(timestamp);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTime()).isEqualTo(timestamp);
  }

  @Test
  void shouldReturnNullForNullDateValue() {
    // When
    Date result = DateTimeUtil.parseDate(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullForEmptyStringDateValue() {
    // Given
    String emptyString = "";

    // When
    Date result = DateTimeUtil.parseDate(emptyString);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullForWhitespaceOnlyStringDateValue() {
    // Given
    String whitespaceString = "   ";

    // When
    Date result = DateTimeUtil.parseDate(whitespaceString);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void shouldThrowExceptionForInvalidDateType() {
    // Given
    Integer invalidType = 123;

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDate(invalidType)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid date type: Integer");
  }

  @Test
  void shouldThrowExceptionForInvalidStringDateFormat() {
    // Given
    String invalidDate = "invalid-date-format";

    // When & Then
    assertThatThrownBy(() -> DateTimeUtil.parseDate(invalidDate)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid date format: " + invalidDate);
  }
}