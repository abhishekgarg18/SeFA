package com.sefa.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Logger utility class
 * Converted from Python utils/logger.py
 */
public class Logger {
    
    private static boolean DEBUG = false;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Set debug mode
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public static boolean isDebug() {
        return DEBUG;
    }
    
    /**
     * Log a message
     */
    public static void log(String message) {
        System.out.println(message);
    }
    
    /**
     * Log a formatted message
     */
    public static void log(String format, Object... args) {
        System.out.printf(format + "%n", args);
    }
    
    /**
     * Log a debug message (only if debug mode is enabled)
     */
    public static void debugLog(String message) {
        if (DEBUG) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    /**
     * Log a formatted debug message (only if debug mode is enabled)
     */
    public static void debugLog(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG] " + format + "%n", args);
        }
    }
    
    /**
     * Log an object as JSON (debug mode only)
     */
    public static void debugLogJson(Object obj) {
        if (DEBUG) {
            try {
                String json = objectMapper.writeValueAsString(obj);
                System.out.println("[DEBUG JSON] " + json);
            } catch (Exception e) {
                System.err.println("[DEBUG JSON ERROR] Failed to serialize object: " + e.getMessage());
                System.out.println("[DEBUG] " + obj.toString());
            }
        }
    }
    
    /**
     * Log an error message
     */
    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
    
    /**
     * Log an error with exception
     */
    public static void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + message);
        throwable.printStackTrace();
    }
    
    /**
     * Log a warning message
     */
    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }
} 