package com.sefa.utils;

import com.sefa.models.Organization;
import java.util.HashMap;
import java.util.Map;

/**
 * Ticker mapping configurations
 * Converted from Python utils/ticker_mapping.py
 */
public class TickerMapping {
    
    private static final Map<String, Organization> TICKER_ORG_INFO = new HashMap<>();
    private static final Map<String, String> TICKER_CURRENCY_INFO = new HashMap<>();
    
    static {
        initializeTickerMappings();
    }
    
    private static void initializeTickerMappings() {
        // Adobe organization info
        Organization adobeOrg = new Organization(
            "2 - United States",
            "Adobe Incorporation", 
            "345 Park Avenue San Jose, CA",
            "Listed",
            "95110"
        );
        
        TICKER_ORG_INFO.put("adbe", adobeOrg);
        TICKER_CURRENCY_INFO.put("adbe", "USD");
    }
    
    /**
     * Get organization information for a ticker
     */
    public static Organization getTickerOrgInfo(String ticker) {
        Organization org = TICKER_ORG_INFO.get(ticker.toLowerCase());
        if (org == null) {
            throw new IllegalArgumentException("No organization info found for ticker: " + ticker);
        }
        return org;
    }
    
    /**
     * Get currency information for a ticker
     */
    public static String getTickerCurrencyInfo(String ticker) {
        String currency = TICKER_CURRENCY_INFO.get(ticker.toLowerCase());
        if (currency == null) {
            throw new IllegalArgumentException("No currency info found for ticker: " + ticker);
        }
        return currency;
    }
    
    /**
     * Check if ticker is supported
     */
    public static boolean isTickerSupported(String ticker) {
        return TICKER_ORG_INFO.containsKey(ticker.toLowerCase());
    }
    
    /**
     * Add new ticker mapping
     */
    public static void addTickerMapping(String ticker, Organization org, String currency) {
        TICKER_ORG_INFO.put(ticker.toLowerCase(), org);
        TICKER_CURRENCY_INFO.put(ticker.toLowerCase(), currency);
    }
    
    /**
     * Get all supported tickers
     */
    public static String[] getSupportedTickers() {
        return TICKER_ORG_INFO.keySet().toArray(new String[0]);
    }
} 