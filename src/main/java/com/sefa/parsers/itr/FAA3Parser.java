package com.sefa.parsers.itr;

import com.sefa.models.*;
import com.sefa.utils.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FAA3 Parser for ITR Schedule FA
 * Converted from Python parser/itr/faa3_parser.py
 */
public class FAA3Parser {
    
    /**
     * Parse organization purchases for a specific ticker
     */
    public static List<FAA3> parseOrgPurchases(String ticker, String calendarMode, 
                                              List<Purchase> purchases, int assessmentYear, 
                                              String outputFolderAbsPath) throws IOException {
        
        long[] calendarRange = DateUtils.calendarRange(calendarMode, assessmentYear);
        long startTimeInMs = calendarRange[0];
        long endTimeInMs = calendarRange[1];
        
        Organization org = TickerMapping.getTickerOrgInfo(ticker);
        String currencyCode = TickerMapping.getTickerCurrencyInfo(ticker);
        
        // Filter purchases into before and after periods
        List<Purchase> beforePurchases = purchases.stream()
            .filter(purchase -> purchase.getDate().getTimeInMillis() < startTimeInMs)
            .collect(Collectors.toList());
        
        List<Purchase> afterPurchases = purchases.stream()
            .filter(purchase -> purchase.getDate().getTimeInMillis() >= startTimeInMs && 
                              purchase.getDate().getTimeInMillis() <= endTimeInMs)
            .collect(Collectors.toList());
        
        // Calculate totals
        double previousSum = beforePurchases.stream()
            .mapToDouble(Purchase::getQuantity)
            .sum();
        
        Logger.log("%s: Previous period(before %s) total share = %.2f",
            ticker, DateUtils.displayTime(startTimeInMs), previousSum);
        
        double afterSum = afterPurchases.stream()
            .mapToDouble(Purchase::getQuantity)
            .sum();
        
        Logger.log("%s: This period(from %s to %s) total share = %.2f",
            ticker, DateUtils.displayTime(startTimeInMs), DateUtils.displayTime(endTimeInMs), afterSum);
        
        List<FAA3> faEntries = new ArrayList<>();
        
        // Process closing prices and rates
        String beforePurchasesLastDate = String.format("31-Dec-%d", assessmentYear - 2);
        DateObj beforePurchaseDate = DateUtils.parseNamedMon(beforePurchasesLastDate);
        
        double closingSharePrice = ShareDataUtils.getClosingPrice(ticker, endTimeInMs);
        double closingInrRate = ShareDataUtils.getUsdInrRate(endTimeInMs);
        double closingInrPrice = closingSharePrice * closingInrRate;
        
        Logger.log("%s: Closing price(INR) = %.2f, closing_share_price(%s) = %.2f closing_rate(INR) = %.2f",
            ticker, closingInrPrice, currencyCode, closingSharePrice, closingInrRate);
        
        double fmvPriceOnStart = ShareDataUtils.getFmv(ticker, beforePurchaseDate.getTimeInMillis());
        Logger.log("%s: Queried FMV on %s is %.2f. This is used for accumulated sum for previous purchases",
            ticker, beforePurchasesLastDate, fmvPriceOnStart);
        
        // Add entry for previous purchases if any
        if (previousSum != 0) {
            double previousPurchasePrice = previousSum * fmvPriceOnStart * closingInrRate;
            double previousPeakPrice = previousSum * ShareDataUtils.getPeakPriceInInr(ticker, startTimeInMs, endTimeInMs);
            double previousClosingPrice = previousSum * closingInrPrice;
            
            Purchase aggregatedPurchase = new Purchase(
                beforePurchaseDate,
                new Price(fmvPriceOnStart, currencyCode),
                previousSum,
                ticker
            );
            
            faEntries.add(new FAA3(
                org,
                aggregatedPurchase,
                previousPurchasePrice,
                previousPeakPrice,
                previousClosingPrice,
                0.0 // No sales for accumulated previous purchases
            ));
        }
        
        // Process individual purchases from the current period
        for (Purchase purchase : afterPurchases) {
            double purchasePrice = purchase.getQuantity() * purchase.getPurchaseFmv().getPrice() * closingInrRate;
            double peakPrice = purchase.getQuantity() * ShareDataUtils.getPeakPriceInInr(
                ticker, purchase.getDate().getTimeInMillis(), endTimeInMs);
            double closingPrice = purchase.getQuantity() * closingInrPrice;
            
            faEntries.add(new FAA3(
                org,
                purchase,
                purchasePrice,
                peakPrice,
                closingPrice,
                0.0 // No sales tracking implemented yet
            ));
        }
        
        // Write output files
        String tickerOutputPath = Paths.get(outputFolderAbsPath, ticker).toString();
        
        // Write JSON file
        FileUtils.writeToFile(tickerOutputPath, "raw_fa_entries.json", faEntries, true);
        
        // Write CSV file with updated format
        String[] headers = {
            "Country",
            "Name of Entity", 
            "Address of Entity",
            "Zip Code",
            "Nature of Entity",
            "Date of Acquisition",
            "Initial Investment",
            "Peak Investment",
            "Closing Balance",
            "Total Gross Amount",
            "Sales Proceeds or Redemption during the year"
        };
        
        List<String[]> csvData = faEntries.stream()
            .map(entry -> new String[]{
                entry.getOrg().getCountryName(),
                entry.getOrg().getName(),
                entry.getOrg().getAddress(),
                entry.getOrg().getZipCode(),
                entry.getOrg().getNature(),
                entry.getPurchase().getDate().getDispTime(),
                String.valueOf(Math.round(entry.getPurchasePrice())),
                String.valueOf(Math.round(entry.getPeakPrice())),
                String.valueOf(Math.round(entry.getClosingPrice())),
                String.valueOf(Math.round(entry.getPeakPrice())), // Total Gross Amount = Peak Investment
                String.valueOf(Math.round(entry.getSalesProceeds())) // Sales Proceeds from model
            })
            .collect(Collectors.toList());
        
        FileUtils.writeCsvToFile(tickerOutputPath, "fa_entries.csv", headers, csvData, true, true);
        
        return faEntries;
    }
    
    /**
     * Process all purchases and generate FA entries for each ticker
     */
    public static void processFAA3(List<Purchase> purchases, String outputFolder, int assessmentYear, String calendarMode) {
        Logger.log("Processing FAA3 entries...");
        
        // Group purchases by ticker
        Map<String, List<Purchase>> tickerPurchases = purchases.stream()
            .collect(Collectors.groupingBy(Purchase::getTicker));
        
        for (Map.Entry<String, List<Purchase>> entry : tickerPurchases.entrySet()) {
            String ticker = entry.getKey();
            List<Purchase> tickerPurchaseList = entry.getValue();
            
            try {
                // Parse org purchases for this ticker using the existing method
                parseOrgPurchases(ticker, calendarMode, tickerPurchaseList, assessmentYear, outputFolder);
            } catch (IOException e) {
                Logger.error("Failed to process ticker " + ticker + ": " + e.getMessage());
            }
        }
        
        Logger.log("Processing completed successfully!");
    }
    
    /**
     * Main parse method that processes all data regardless of assessment year
     */
    public static void parse(String calendarMode, List<Purchase> purchases, 
                           int assessmentYear, String outputFolderAbsPath) throws IOException {
        
        // Create output directory
        FileUtils.createDirectory(outputFolderAbsPath);
        
        // Group purchases by ticker
        Map<String, List<Purchase>> groupedPurchases = purchases.stream()
            .collect(Collectors.groupingBy(Purchase::getTicker));
        
        // Collect all FAA3 entries from all tickers - process ALL data
        List<FAA3> allFaEntries = new ArrayList<>();
        
        // Process each ticker and collect entries for ALL purchases
        for (Map.Entry<String, List<Purchase>> entry : groupedPurchases.entrySet()) {
            String ticker = entry.getKey();
            List<Purchase> tickerPurchases = entry.getValue();
            
            List<FAA3> tickerEntries = processAllPurchasesForTicker(ticker, tickerPurchases);
            allFaEntries.addAll(tickerEntries);
        }
        
        // Sort all entries by acquisition date
        allFaEntries.sort(Comparator.comparing(entry -> entry.getPurchase().getDate().getTimeInMillis()));
        
        String[] headers = {
            "Country/Region name",
            "Country Name and Code", 
            "Name of entity",
            "Address of entity",
            "ZIP Code",
            "Nature of entity",
            "Date of acquiring the interest",
            "Initial value of the investment",
            "Peak value of investment during the Period",
            "Closing balance",
            "Total gross amount paid/credited with respect to the holding during the period",
            "Total gross proceeds from sale or redemption of investment during the period"
        };
        
        List<String[]> csvData = new ArrayList<>();
        for (int i = 0; i < allFaEntries.size(); i++) {
            FAA3 entry = allFaEntries.get(i);
            String[] row = new String[]{
                String.valueOf(i + 1), // Sr. No. (1, 2, 3, ...)
                "2", // Country code for USA
                entry.getOrg().getName() + " (" + entry.getPurchase().getTicker().toUpperCase() + ")", // Company name + ticker
                entry.getOrg().getAddress().replace(",", ""), // Address without commas
                entry.getOrg().getZipCode().length() > 8 ? entry.getOrg().getZipCode().substring(0, 8) : entry.getOrg().getZipCode(), // Max 8 chars ZIP
                "Company", // Nature of entity
                entry.getPurchase().getDate().getDispTime(), // Date in YYYY-MM-DD format (already correct)
                String.valueOf(Math.round(entry.getPurchasePrice())),
                String.valueOf(Math.round(entry.getPeakPrice())),
                String.valueOf(Math.round(entry.getClosingPrice())),
                String.valueOf(Math.round(entry.getSalesProceeds())), // Total gross amount (sale proceeds if sold)
                String.valueOf(Math.round(entry.getSalesProceeds())) // Sales proceeds
            };
            csvData.add(row);
        }
        
        // Write to single combined file
        FileUtils.writeCsvToFile(outputFolderAbsPath, "all_fa_entries.csv", headers, csvData, true, true);
        
        Logger.log("All entries output file created at %s/all_fa_entries.csv", outputFolderAbsPath);
        Logger.log("Total entries processed: %d", allFaEntries.size());
    }
    
    /**
     * Process all purchases for a ticker without any year filtering
     */
    private static List<FAA3> processAllPurchasesForTicker(String ticker, List<Purchase> purchases) {
        
        Organization org = TickerMapping.getTickerOrgInfo(ticker);
        String currencyCode = TickerMapping.getTickerCurrencyInfo(ticker);
        
        List<FAA3> faEntries = new ArrayList<>();
        
        // Define Assessment Year 2024-25 boundaries
        long fyStartTime = DateUtils.parseYyyyMmDd("2024-04-01").getTimeInMillis(); // 1-Apr-2024
        long fyEndTime = DateUtils.parseYyyyMmDd("2025-03-31").getTimeInMillis();   // 31-Mar-2025
        
        // Get closing values at end of AY (31-Mar-2025)
        double closingSharePrice = ShareDataUtils.getClosingPrice(ticker, fyEndTime);
        double closingInrRate = ShareDataUtils.getUsdInrRate(fyEndTime);
        double closingInrPrice = closingSharePrice * closingInrRate;
        
        Logger.log("%s: Processing %d purchases. Closing price(INR) = %.2f at 31-Mar-2025, closing_share_price(%s) = %.2f closing_rate(INR) = %.2f",
            ticker, purchases.size(), closingInrPrice, currencyCode, closingSharePrice, closingInrRate);
        
        for (Purchase purchase : purchases) {
            long purchaseTime = purchase.getDate().getTimeInMillis();
            
            // Calculate Initial Value using USD/INR rate at purchase date (not closing date)
            double purchaseInrRate = ShareDataUtils.getUsdInrRate(purchaseTime);
            double purchasePrice = purchase.getQuantity() * purchase.getPurchaseFmv().getPrice() * purchaseInrRate;
            
            // Closing balance using end of AY rates
            double closingPrice = purchase.getQuantity() * closingInrPrice;
            
            double peakPrice;
            
            if (purchaseTime >= fyStartTime) {
                // Case 1: Purchase after 1-Apr-2024
                if (purchaseTime <= fyEndTime) {
                    // Purchase within FY 2024-25 - peak from purchase date to 31-Mar-2025
                    peakPrice = purchase.getQuantity() * ShareDataUtils.getPeakPriceInInr(
                        ticker, purchaseTime, fyEndTime);
                } else {
                    // Purchase after 31-Mar-2025 - peak for entire FY 2024-25
                    peakPrice = purchase.getQuantity() * ShareDataUtils.getPeakPriceInInr(
                        ticker, fyStartTime, fyEndTime);
                }
            } else {
                // Case 2: Purchase before 1-Apr-2024 - peak for entire FY 2024-25
                peakPrice = purchase.getQuantity() * ShareDataUtils.getPeakPriceInInr(
                    ticker, fyStartTime, fyEndTime);
            }
            
            // Total gross amount should be dividends/income received (currently 0 as we don't track dividends)
            double totalGrossAmount = 0.0; // TODO: Add dividend tracking if needed
            
            // Sales proceeds - set to 0 for manual calculation
            double saleProceeds = 0.0;
            
            faEntries.add(new FAA3(
                org,
                purchase,
                purchasePrice,
                peakPrice,
                closingPrice,
                saleProceeds // Will be calculated manually
            ));
        }
        
        return faEntries;
    }
} 