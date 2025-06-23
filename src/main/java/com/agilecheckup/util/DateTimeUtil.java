package com.agilecheckup.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Utility class for parsing datetime strings into LocalDateTime objects.
 * 
 * This utility handles various datetime formats commonly used in the application:
 * - ISO8601 with timezone (e.g., "2025-06-19T17:54:27.862Z")
 * - ISO8601 without timezone (e.g., "2025-06-19T17:54:27")
 * - Simple LocalDateTime format
 * 
 * Follows clean code principles and provides consistent datetime parsing across the application.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a datetime string into a LocalDateTime object.
     * 
     * Supports multiple formats:
     * 1. ISO8601 with timezone (converts to LocalDateTime by discarding timezone info)
     * 2. Simple LocalDateTime format
     * 
     * @param dateTimeString the datetime string to parse
     * @return LocalDateTime object
     * @throws DateTimeParseException if the string cannot be parsed
     * @throws IllegalArgumentException if the string is null
     */
    public static LocalDateTime parseDateTime(String dateTimeString) throws DateTimeParseException {
        if (dateTimeString == null) {
            throw new IllegalArgumentException("DateTime string cannot be null");
        }

        try {
            // First try to parse as ISO8601 with timezone (from frontend)
            if (isISO8601WithTimezone(dateTimeString)) {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return offsetDateTime.toLocalDateTime();
            }
            
            // Fall back to simple LocalDateTime format
            return LocalDateTime.parse(dateTimeString);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                "Unable to parse datetime: " + dateTimeString + 
                ". Expected ISO8601 format (e.g., '2025-06-19T17:54:27.862Z') or LocalDateTime format (e.g., '2025-06-19T17:54:27')",
                dateTimeString,
                e.getErrorIndex()
            );
        }
    }

    /**
     * Checks if a datetime string is in ISO8601 format with timezone information.
     * 
     * @param dateTimeString the string to check
     * @return true if the string appears to be ISO8601 with timezone
     */
    private static boolean isISO8601WithTimezone(String dateTimeString) {
        return dateTimeString.contains("T") && 
               (dateTimeString.endsWith("Z") || 
                (dateTimeString.contains("+") && dateTimeString.lastIndexOf("+") > dateTimeString.indexOf("T")) ||
                (dateTimeString.contains("-") && dateTimeString.lastIndexOf("-") > dateTimeString.indexOf("T")));
    }

    /**
     * Parses a date/datetime value into a Date object.
     * 
     * Supports multiple input types and formats:
     * 1. String: ISO8601 with timezone, simple date format (yyyy-MM-dd), or LocalDateTime format
     * 2. Long: timestamp in milliseconds
     * 3. null: returns null
     * 
     * @param dateValue the date value to parse (String, Long, or null)
     * @return Date object or null if input is null
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    public static Date parseDate(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof String) {
            String dateString = (String) dateValue;
            if (dateString.trim().isEmpty()) {
                return null;
            }
            
            try {
                // If it contains 'T', treat it as datetime
                if (dateString.contains("T")) {
                    LocalDateTime localDateTime = parseDateTime(dateString);
                    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                } else {
                    // Simple date format (yyyy-MM-dd)
                    LocalDateTime localDateTime = LocalDateTime.parse(dateString + "T00:00:00");
                    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + dateValue + 
                    ". Expected formats: ISO8601 (e.g., '2025-06-19T17:54:27.862Z'), LocalDateTime (e.g., '2025-06-19T17:54:27'), or simple date (e.g., '2025-06-19')", e);
            }
        } else if (dateValue instanceof Long) {
            // Handle timestamp in milliseconds
            return new Date((Long) dateValue);
        }
        
        throw new IllegalArgumentException("Invalid date type: " + dateValue.getClass().getSimpleName() + 
            ". Expected String, Long, or null");
    }
}