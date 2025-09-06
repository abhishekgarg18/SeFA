package com.sefa.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Share data utilities for stock price and currency rate processing
 * Converted from Python utils/share_data_utils.py
 */
public class ShareDataUtils {
    
    public static class TimedFmv {
        private final long entryTimeInMillis;
        private final double fmv;
        
        public TimedFmv(long entryTimeInMillis, double fmv) {
            this.entryTimeInMillis = entryTimeInMillis;
            this.fmv = fmv;
        }
        
        public long getEntryTimeInMillis() {
            return entryTimeInMillis;
        }
        
        public double getFmv() {
            return fmv;
        }
    }
    
    public static class TimedFmvWithInrRate extends TimedFmv {
        private final double inrRate;
        
        public TimedFmvWithInrRate(long entryTimeInMillis, double fmv, double inrRate) {
            super(entryTimeInMillis, fmv);
            this.inrRate = inrRate;
        }
        
        public double getInrRate() {
            return inrRate;
        }
    }
    
    private static final Map<String, List<TimedFmv>> priceMapCache = new HashMap<>();
    private static List<TimedFmv> usdInrCache = new ArrayList<>();
    
    /**
     * Validate dates for FMV lookup
     */
    private static void validateDates(long historicEntryTimeInMs, long desiredPurchaseTimeInMs, long usedFmvTimeInMs) {
        if (historicEntryTimeInMs > desiredPurchaseTimeInMs) {
            throw new IllegalArgumentException(
                String.format("Historical FMV date %s can NOT be newer than purchase date = %s",
                    DateUtils.logTimestamp(historicEntryTimeInMs),
                    DateUtils.logTimestamp(desiredPurchaseTimeInMs))
            );
        }
        
        long daysDiff = (DateUtils.lastWorkDayInMs(desiredPurchaseTimeInMs) - historicEntryTimeInMs) / (24 * 60 * 60 * 1000);
        
        if (daysDiff > 0) {
            String msg = String.format(
                "Historical FMV at %s was NOT available(maybe due to Public Holiday or weekends) last available data is %d days old(on %s)",
                DateUtils.logTimestamp(desiredPurchaseTimeInMs),
                (int) daysDiff,
                DateUtils.displayTime(historicEntryTimeInMs)
            );
            Logger.log(msg);
            Logger.log("Hence using the next available FMV at %s", DateUtils.logTimestamp(usedFmvTimeInMs));
        }
    }
    
    /**
     * Initialize USD/INR exchange rate cache
     */
    private static List<TimedFmv> initUsdInrMap() {
        if (usdInrCache.isEmpty()) {
            Logger.log("Parsing USD/INR exchange rate map");
            
            String scriptPath = System.getProperty("user.dir");
            Path usdInrPath = Paths.get(scriptPath, "historic_data", "usd_inr_price_history.csv");
            
            if (!FileUtils.fileExists(usdInrPath.toString())) {
                throw new IllegalArgumentException("USD/INR historical data NOT present at " + usdInrPath);
            }
            
            try (CSVReader reader = new CSVReader(new FileReader(usdInrPath.toFile()))) {
                List<String[]> records = reader.readAll();
                
                // Skip header row
                for (int i = 1; i < records.size(); i++) {
                    String[] row = records.get(i);
                    long entryTimeInMs = DateUtils.parseDateFromUsdInrFormat(row[0].replace("\"", ""));
                    String priceStr = row[1].replace("\"", "").replace(",", "");
                    double price = Double.parseDouble(priceStr);
                    
                    usdInrCache.add(new TimedFmv(entryTimeInMs, price));
                }
                
                // Sort by date for efficient lookup
                usdInrCache.sort(Comparator.comparingLong(TimedFmv::getEntryTimeInMillis));
                
            } catch (IOException | CsvException e) {
                throw new RuntimeException("Failed to read USD/INR data: " + e.getMessage(), e);
            }
        }
        
        return usdInrCache;
    }
    
    /**
     * Initialize price map for a ticker
     */
    private static List<TimedFmv> initMap(String ticker) {
        if (!priceMapCache.containsKey(ticker)) {
            Logger.log("Parsing FMV price map for ticker = %s", ticker);
            
            List<TimedFmv> tickerPriceMap = new ArrayList<>();
            String scriptPath = System.getProperty("user.dir");
            Path historicSharePath;
            
            // Use the new adobe_price_history.csv file for ADBE
            if ("adbe".equalsIgnoreCase(ticker)) {
                historicSharePath = Paths.get(scriptPath, "historic_data", "adobe_price_history.csv");
            } else {
                // Fallback to old structure for other tickers
                historicSharePath = Paths.get(scriptPath, "historic_data", "shares", ticker.toLowerCase(), "data.csv");
            }
            
            if (!FileUtils.fileExists(historicSharePath.toString())) {
                throw new IllegalArgumentException(
                    String.format("Historic share data for share %s NOT present at %s", ticker, historicSharePath)
                );
            }
            
            try (CSVReader reader = new CSVReader(new FileReader(historicSharePath.toFile()))) {
                List<String[]> records = reader.readAll();
                
                // Skip header row
                for (int i = 1; i < records.size(); i++) {
                    String[] row = records.get(i);
                    long entryTimeInMs;
                    double price;
                    
                    if ("adbe".equalsIgnoreCase(ticker)) {
                        // Handle adobe_price_history.csv format
                        entryTimeInMs = DateUtils.parseDateFromAdobeFormat(row[0]);
                        // Clean the Close/Last value (remove $ sign if present)
                        String closePriceStr = row[1].replace("$", "").replace(",", "");
                        price = Double.parseDouble(closePriceStr);
                    } else {
                        // Handle old data.csv format
                        entryTimeInMs = DateUtils.parseYyyyMmDd(row[0]).getTimeInMillis();
                        price = Double.parseDouble(row[4]); // Close column
                    }
                    
                    tickerPriceMap.add(new TimedFmv(entryTimeInMs, price));
                }
                
                // Sort by date for efficient lookup
                tickerPriceMap.sort(Comparator.comparingLong(TimedFmv::getEntryTimeInMillis));
                priceMapCache.put(ticker, tickerPriceMap);
                
            } catch (IOException | CsvException e) {
                throw new RuntimeException("Failed to read share data for " + ticker + ": " + e.getMessage(), e);
            }
        }
        
        return priceMapCache.get(ticker);
    }
    
    /**
     * Get Fair Market Value for a ticker at a specific time
     */
    public static double getFmv(String ticker, long purchaseTimeInMs) {
        Logger.debugLog("%s: Querying FMV at %s", ticker, DateUtils.displayTime(purchaseTimeInMs));
        
        List<TimedFmv> priceMap = initMap(ticker);
        TimedFmv previousEntry = null;
        
        for (TimedFmv entry : priceMap) {
            long entryTimeInMs = entry.getEntryTimeInMillis();
            if (entryTimeInMs >= purchaseTimeInMs) {
                if (entryTimeInMs > purchaseTimeInMs && previousEntry != null) {
                    validateDates(previousEntry.getEntryTimeInMillis(), purchaseTimeInMs, entryTimeInMs);
                    return entry.getFmv();
                }
                return entry.getFmv();
            }
            previousEntry = entry;
        }
        
        // Updated error message to reflect new file locations
        String tickerSharePrice = "adbe".equalsIgnoreCase(ticker) 
            ? "historic_data/adobe_price_history.csv"
            : String.join("/", "historic_data", "shares", ticker, "data.csv");
        
        throw new IllegalArgumentException(
            String.format("No FMV data for share ticker %s in %s for date %s",
                ticker, tickerSharePrice, DateUtils.logTimestamp(purchaseTimeInMs))
        );
    }
    
    /**
     * Get USD to INR exchange rate for a given timestamp
     */
    public static double getUsdInrRate(long timeInMs) {
        List<TimedFmv> usdInrData = initUsdInrMap();
        TimedFmv previousEntry = null;
        
        for (TimedFmv entry : usdInrData) {
            long entryTimeInMs = entry.getEntryTimeInMillis();
            if (entryTimeInMs >= timeInMs) {
                if (entryTimeInMs > timeInMs) {
                    return previousEntry != null ? previousEntry.getFmv() : entry.getFmv();
                }
                return entry.getFmv();
            }
            previousEntry = entry;
        }
        
        // If no future data found, use the last available rate
        if (previousEntry != null) {
            return previousEntry.getFmv();
        }
        
        throw new IllegalArgumentException(
            "No USD/INR rate data available for date " + DateUtils.logTimestamp(timeInMs)
        );
    }
    
    /**
     * Get closing price for a ticker at end time
     */
    public static double getClosingPrice(String ticker, long endTimeInMs) {
        List<TimedFmv> priceMap = initMap(ticker);
        
        Optional<TimedFmv> result = priceMap.stream()
            .filter(price -> price.getEntryTimeInMillis() <= endTimeInMs)
            .max(Comparator.comparingLong(TimedFmv::getEntryTimeInMillis));
        
        return result.map(TimedFmv::getFmv)
            .orElseThrow(() -> new IllegalArgumentException("No closing price data found for " + ticker));
    }
    
    /**
     * Get peak price in INR within a time range
     */
    public static double getPeakPriceInInr(String ticker, long startTimeInMs, long endTimeInMs) {
        if (startTimeInMs > endTimeInMs) {
            throw new IllegalArgumentException(
                String.format("start_time_in_ms = %d is greater than equal to end_time_in_ms = %d",
                    startTimeInMs, endTimeInMs)
            );
        }
        
        List<TimedFmv> priceMap = initMap(ticker);
        String currencyCode = TickerMapping.getTickerCurrencyInfo(ticker);
        
        // Filter prices within the time range
        List<TimedFmv> filteredPrices = priceMap.stream()
            .filter(price -> price.getEntryTimeInMillis() >= startTimeInMs && 
                           price.getEntryTimeInMillis() <= endTimeInMs)
            .collect(Collectors.toList());
        
        // Convert to prices with INR rates
        List<TimedFmvWithInrRate> priceMapWithInrRate = filteredPrices.stream()
            .map(price -> {
                double inrRate = "USD".equals(currencyCode) 
                    ? getUsdInrRate(price.getEntryTimeInMillis())
                    : 1.0; // Fallback for other currencies
                return new TimedFmvWithInrRate(price.getEntryTimeInMillis(), price.getFmv(), inrRate);
            })
            .collect(Collectors.toList());
        
        // Find maximum value
        TimedFmvWithInrRate maxValue = priceMapWithInrRate.stream()
            .max(Comparator.comparingDouble(price -> price.getFmv() * price.getInrRate()))
            .orElseThrow(() -> new IllegalArgumentException("No price data found in the given range"));
        
        double peakPriceInInr = maxValue.getFmv() * maxValue.getInrRate();
        
        Logger.debugLogJson(Map.of(
            "start_time", DateUtils.displayTime(startTimeInMs),
            "end_time", DateUtils.displayTime(endTimeInMs),
            "max_fmv($)", maxValue.getFmv(),
            "max_fmv($)_at", DateUtils.displayTime(maxValue.getEntryTimeInMillis()),
            "inr_conversion_rate", maxValue.getInrRate(),
            "effective_price(INR)", peakPriceInInr
        ));
        
        Logger.log("Peak price for ticker = %s from %s to %s is %.2f INR at rate %.2f INR/USD",
            ticker, DateUtils.displayTime(startTimeInMs), DateUtils.displayTime(endTimeInMs),
            peakPriceInInr, maxValue.getInrRate());
        
        return peakPriceInInr;
    }
} 