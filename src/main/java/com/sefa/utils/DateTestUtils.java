package com.sefa.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTestUtils {
    
    public static void main(String[] args) {
        String dateStr = "30-JUN-2020";
        System.out.println("Testing date parsing for: " + dateStr);
        
        String[] patterns = {
            "dd-MMM-yyyy",
            "d-MMM-yyyy",
            "dd-MMM-yy",
            "d-MMM-yy"
        };
        
        Locale[] locales = {Locale.US, Locale.ENGLISH, Locale.getDefault()};
        
        for (Locale locale : locales) {
            System.out.println("\nTesting with locale: " + locale);
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    System.out.println("  SUCCESS with pattern " + pattern + ": " + date);
                } catch (Exception e) {
                    System.out.println("  FAILED with pattern " + pattern + ": " + e.getMessage());
                }
            }
        }
        
        // Try with uppercase
        System.out.println("\nTesting with uppercase: " + dateStr.toUpperCase());
        for (Locale locale : locales) {
            System.out.println("\nTesting with locale: " + locale);
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
                    LocalDate date = LocalDate.parse(dateStr.toUpperCase(), formatter);
                    System.out.println("  SUCCESS with pattern " + pattern + ": " + date);
                } catch (Exception e) {
                    System.out.println("  FAILED with pattern " + pattern + ": " + e.getMessage());
                }
            }
        }
    }
} 