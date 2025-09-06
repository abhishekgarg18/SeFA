package com.sefa.utils;

import com.sefa.models.DateObj;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * Date utility functions
 * Converted from Python utils/date_utils.py
 */
public class DateUtils {
    
    public static final long ONE_DAY_IN_MS = 24 * 60 * 60 * 1000L;
    
    /**
     * Convert LocalDateTime to epoch milliseconds
     */
    public static long epochInMs(LocalDateTime dt) {
        return dt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
    
    /**
     * Create a DateObj from LocalDateTime and original date string
     */
    private static DateObj createDateObject(LocalDateTime dt, String dateStr) {
        long timeInMillis = epochInMs(dt);
        String dispTime = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH));
        return new DateObj(timeInMillis, dispTime, dateStr);
    }
    
    /**
     * Format time in milliseconds to display format (2020-06-30)
     */
    public static String displayTime(long timeInMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMs), ZoneOffset.UTC);
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH));
    }
    
    /**
     * Create log timestamp string
     */
    public static String logTimestamp(long timeInMs) {
        return displayTime(timeInMs) + "(time in ms = " + timeInMs + ")";
    }
    
    /**
     * Parse named month format (30-JUN-2020) - improved version
     */
    public static DateObj parseNamedMon(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be null or empty");
        }
        
        String cleanDateStr = dateStr.trim();
        
        // Handle the specific format we see in Excel: 30-JUN-2020
        if (cleanDateStr.matches("\\d{1,2}-[A-Z]{3}-\\d{4}")) {
            return parseExcelDateFormat(cleanDateStr);
        }
        
        // Fallback to original logic for other formats
        DateTimeFormatter formatter;
        
        // Try different date formats that might appear in Excel
        String[] patterns = {
            "dd-MMM-yyyy",     
            "d-MMM-yyyy",      
            "dd-MMM-yy",       
            "d-MMM-yy",        
            "MMM d, yyyy",     
            "MMM dd, yyyy",    
            "d/M/yyyy",        
            "dd/MM/yyyy",      
            "MM/dd/yyyy",      
            "yyyy-MM-dd"       
        };
        
        for (String pattern : patterns) {
            try {
                formatter = DateTimeFormatter.ofPattern(pattern, Locale.US);
                LocalDateTime dateTime = LocalDate.parse(cleanDateStr, formatter).atStartOfDay();
                return createDateObject(dateTime, dateStr);
            } catch (Exception e) {
                // Continue to next pattern
            }
        }
        
        throw new IllegalArgumentException("Unable to parse date: " + dateStr + ". Tried patterns: " + String.join(", ", patterns));
    }
    
    /**
     * Parse Excel date format specifically (30-JUN-2020)
     */
    private static DateObj parseExcelDateFormat(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid date format: " + dateStr);
            }
            
            int day = Integer.parseInt(parts[0]);
            String monthStr = parts[1].toUpperCase();
            int year = Integer.parseInt(parts[2]);
            
            // Map month abbreviations to numbers
            Map<String, Integer> monthMap = new HashMap<>();
            monthMap.put("JAN", 1);
            monthMap.put("FEB", 2);
            monthMap.put("MAR", 3);
            monthMap.put("APR", 4);
            monthMap.put("MAY", 5);
            monthMap.put("JUN", 6);
            monthMap.put("JUL", 7);
            monthMap.put("AUG", 8);
            monthMap.put("SEP", 9);
            monthMap.put("OCT", 10);
            monthMap.put("NOV", 11);
            monthMap.put("DEC", 12);
            
            Integer month = monthMap.get(monthStr);
            if (month == null) {
                throw new IllegalArgumentException("Unknown month abbreviation: " + monthStr);
            }
            
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, 0, 0, 0);
            return createDateObject(dateTime, dateStr);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Excel date format: " + dateStr + " - " + e.getMessage());
        }
    }
    
    /**
     * Parse MM/DD/YYYY format
     */
    public static DateObj parseMmDd(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDateTime dateTime = LocalDate.parse(dateStr, formatter).atStartOfDay();
        return createDateObject(dateTime, dateStr);
    }
    
    /**
     * Parse YYYY-MM-DD format
     */
    public static DateObj parseYyyyMmDd(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime dateTime = LocalDate.parse(dateStr, formatter).atStartOfDay();
        return createDateObject(dateTime, dateStr);
    }
    
    /**
     * Parse date from adobe_price_history.csv format: MM/DD/YYYY
     */
    public static long parseDateFromAdobeFormat(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDateTime dateTime = LocalDate.parse(dateStr, formatter).atStartOfDay();
        return epochInMs(dateTime);
    }
    
    /**
     * Parse date from usd_inr_price_history.csv format: DD-MM-YYYY
     */
    public static long parseDateFromUsdInrFormat(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDateTime dateTime = LocalDate.parse(dateStr, formatter).atStartOfDay();
        return epochInMs(dateTime);
    }
    
    /**
     * Get last working day in milliseconds (Friday if weekend)
     */
    public static long lastWorkDayInMs(long timeInMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMs), ZoneOffset.UTC);
        DayOfWeek weekday = dt.getDayOfWeek();
        
        if (weekday == DayOfWeek.SATURDAY) {
            return epochInMs(dt.minusDays(1));
        } else if (weekday == DayOfWeek.SUNDAY) {
            return epochInMs(dt.minusDays(2));
        } else {
            return timeInMs;
        }
    }
    
    /**
     * Get calendar range for the given mode and year
     * Returns [startTimeMs, endTimeMs]
     */
    public static long[] calendarRange(String calendarMode, int year) {
        LocalDateTime startTime;
        LocalDateTime endTime;
        
        if ("calendar".equals(calendarMode)) {
            startTime = LocalDateTime.of(year - 1, 1, 1, 0, 0);
            endTime = LocalDateTime.of(year - 1, 12, 31, 23, 59, 59);
        } else if ("financial".equals(calendarMode)) {
            startTime = LocalDateTime.of(year - 1, 4, 1, 0, 0);
            endTime = LocalDateTime.of(year, 3, 31, 23, 59, 59);
        } else {
            throw new IllegalArgumentException(
                "Unsupported calendar_mode = " + calendarMode + " for year = " + year
            );
        }
        
        return new long[]{epochInMs(startTime), epochInMs(endTime)};
    }
} 