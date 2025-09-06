package com.sefa.parsers.etrade;

import com.sefa.models.*;
import com.sefa.utils.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * ETRADE Benefit History Excel parser
 * Converted from Python parser/demat/etrade/etrade_benefit_history_parser.py
 */
public class EtradeBenefitHistoryParser {
    
    // Constants
    private static final String ESPP_SHEET_NAME = "ESPP";
    private static final String RSU_SHEET_NAME = "Restricted Stock";
    private static boolean DEBUG = true; // Temporarily enable for debugging
    
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }
    
    /**
     * Parse ESPP row from Excel data - improved version
     */
    private static Purchase parseEsppRow(Row row, Map<String, Integer> columnMap) {
        if (row == null || row.getCell(0) == null) return null;
        
        try {
            String recordType = getCellStringValue(row.getCell(columnMap.getOrDefault("Record Type", 1)));
            
            // Skip debug output in production
            
            if (!"Purchase".equals(recordType)) {
                return null;
            }
            
            String purchaseDate = getCellStringValue(row.getCell(columnMap.getOrDefault("Purchase Date", 3)));
            String symbol = getCellStringValue(row.getCell(columnMap.getOrDefault("Symbol", 4)));
            String fmvStr = getCellStringValue(row.getCell(columnMap.getOrDefault("Purchase Date FMV", 18)));
            
            // Get quantity - use "Purchased Qty." column
            Cell quantityCell = row.getCell(columnMap.getOrDefault("Purchased Qty.", 17));
            double quantity = 0.0;
            if (quantityCell != null) {
                if (quantityCell.getCellType() == CellType.NUMERIC) {
                    quantity = quantityCell.getNumericCellValue();
                } else if (quantityCell.getCellType() == CellType.STRING) {
                    String qtyStr = quantityCell.getStringCellValue().replaceAll("[^0-9.]", "");
                    if (!qtyStr.isEmpty()) {
                        quantity = Double.parseDouble(qtyStr);
                    }
                }
            }
            
            if (quantity <= 0) {
                return null;
            }
            
            // Clean FMV value (remove $ sign and other formatting)
            double fmv = 0.0;
            if (fmvStr != null && !fmvStr.isEmpty()) {
                String cleanFmv = fmvStr.replaceAll("[^0-9.]", "");
                if (!cleanFmv.isEmpty()) {
                    fmv = Double.parseDouble(cleanFmv);
                }
            }
            
            if (fmv <= 0) {
                return null;
            }
            
            // Validate symbol
            if (symbol == null || symbol.trim().isEmpty()) {
                return null;
            }
            
            // Parse date
            DateObj dateObj = DateUtils.parseNamedMon(purchaseDate);
            
            // Get currency
            String currency = TickerMapping.getTickerCurrencyInfo(symbol.toLowerCase());
            
            return new Purchase(
                dateObj,
                new Price(fmv, currency),
                quantity,
                symbol.toLowerCase()
            );
            
        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("Error parsing ESPP row " + row.getRowNum() + ": " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Parse RSU sheet - improved version to handle complex data structure
     */
    private static List<Purchase> parseRsu(Workbook workbook) {
        List<Purchase> purchases = new ArrayList<>();
        
        Sheet sheet = workbook.getSheet(RSU_SHEET_NAME);
        if (sheet == null) {
            String[] alternativeNames = {"RSU", "Restricted Stock", "Stock Awards", "Equity Awards"};
            for (String altName : alternativeNames) {
                sheet = workbook.getSheet(altName);
                if (sheet != null) {
                    break;
                }
            }
            if (sheet == null) {
                return purchases;
            }
        }
        
        Iterator<Row> rowIterator = sheet.iterator();
        Map<String, Integer> columnMap = new HashMap<>();
        
        // Read header row to build column mapping
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            columnMap = buildColumnMap(headerRow);
        }
        
        // First pass: collect all grant information
        Map<String, GrantInfo> grants = new HashMap<>();
        List<VestEvent> vestEvents = new ArrayList<>();
        
        // Process all rows to separate grants and vest events
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String recordType = getCellStringValue(row.getCell(columnMap.getOrDefault("Record Type", 1)));
            String eventType = getCellStringValue(row.getCell(columnMap.getOrDefault("Event Type", 31)));
            
            if ("Grant".equals(recordType)) {
                // This is a grant row - collect grant information
                String symbol = getCellStringValue(row.getCell(columnMap.getOrDefault("Symbol", 3)));
                String grantNumber = getCellStringValue(row.getCell(columnMap.getOrDefault("Grant Number", 17)));
                String grantDate = getCellStringValue(row.getCell(columnMap.getOrDefault("Grant Date", 13)));
                
                if (symbol != null && !symbol.trim().isEmpty()) {
                    grants.put(grantNumber, new GrantInfo(symbol, grantDate, grantNumber));
                }
            } else if ("Event".equals(recordType) && "Shares vested".equals(eventType)) {
                // This is a vest event - collect vest information
                String date = getCellStringValue(row.getCell(columnMap.getOrDefault("Date", 24)));
                String grantNumber = getCellStringValue(row.getCell(columnMap.getOrDefault("Grant Number", 17)));
                String qtyStr = getCellStringValue(row.getCell(columnMap.getOrDefault("Qty. or Amount", 22)));
                
                // Try multiple FMV columns
                String fmvStr = getCellStringValue(row.getCell(columnMap.getOrDefault("Est. Market Value", 25)));
                if (fmvStr == null || fmvStr.trim().isEmpty()) {
                    fmvStr = getCellStringValue(row.getCell(columnMap.getOrDefault("Taxable Gain", 16)));
                }
                if (fmvStr == null || fmvStr.trim().isEmpty()) {
                    fmvStr = getCellStringValue(row.getCell(columnMap.getOrDefault("Award Price", 4)));
                }
                
                vestEvents.add(new VestEvent(date, grantNumber, qtyStr, fmvStr));
            }
        }
        
        // Second pass: correlate vest events with grants
        for (VestEvent vestEvent : vestEvents) {
            GrantInfo grant = grants.get(vestEvent.grantNumber);
            
            if (grant != null && grant.symbol != null) {
                try {
                    // Parse quantity
                    double quantity = 0.0;
                    if (vestEvent.quantity != null && !vestEvent.quantity.trim().isEmpty()) {
                        String cleanQty = vestEvent.quantity.replaceAll("[^0-9.]", "");
                        if (!cleanQty.isEmpty()) {
                            quantity = Double.parseDouble(cleanQty);
                        }
                    }
                    
                    // Parse FMV
                    double fmv = 0.0;
                    if (vestEvent.fmv != null && !vestEvent.fmv.trim().isEmpty()) {
                        String cleanFmv = vestEvent.fmv.replaceAll("[^0-9.]", "");
                        if (!cleanFmv.isEmpty()) {
                            fmv = Double.parseDouble(cleanFmv);
                        }
                    }
                    
                    if (quantity > 0 && vestEvent.date != null && !vestEvent.date.trim().isEmpty()) {
                        // Parse the MM/DD/YYYY date format
                        DateObj dateObj = DateUtils.parseMmDd(vestEvent.date);
                        String currency = TickerMapping.getTickerCurrencyInfo(grant.symbol.toLowerCase());
                        
                        // If FMV is missing, use stock price lookup
                        if (fmv <= 0) {
                            fmv = ShareDataUtils.getFmv(grant.symbol.toLowerCase(), dateObj.getTimeInMillis());
                        }
                        
                        if (fmv > 0) {
                            Purchase purchase = new Purchase(
                                dateObj,
                                new Price(fmv, currency),
                                quantity,
                                grant.symbol.toLowerCase()
                            );
                            
                            purchases.add(purchase);
                        }
                    }
                } catch (Exception e) {
                    // Silently skip problematic entries
                }
            }
        }
        
        return purchases;
    }
    
    // Helper classes for RSU data correlation
    private static class GrantInfo {
        String symbol;
        String grantDate;
        String grantNumber;
        
        GrantInfo(String symbol, String grantDate, String grantNumber) {
            this.symbol = symbol;
            this.grantDate = grantDate;
            this.grantNumber = grantNumber;
        }
    }
    
    private static class VestEvent {
        String date;
        String grantNumber;
        String quantity;
        String fmv;
        
        VestEvent(String date, String grantNumber, String quantity, String fmv) {
            this.date = date;
            this.grantNumber = grantNumber;
            this.quantity = quantity;
            this.fmv = fmv;
        }
    }
    
    /**
     * Build column mapping from header row
     */
    private static Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        if (headerRow != null) {
            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String headerValue = getCellStringValue(cell);
                if (!headerValue.isEmpty()) {
                    columnMap.put(headerValue, cell.getColumnIndex());
                }
            }
        }
        
        return columnMap;
    }
    
    /**
     * Parse ESPP sheet - improved version
     */
    private static List<Purchase> parseEspp(Workbook workbook) {
        List<Purchase> purchases = new ArrayList<>();
        
        Sheet sheet = workbook.getSheet(ESPP_SHEET_NAME);
        if (sheet == null) {
            String[] alternativeNames = {"ESPP", "Employee Stock Purchase Plan", "Stock Purchase"};
            for (String altName : alternativeNames) {
                sheet = workbook.getSheet(altName);
                if (sheet != null) {
                    break;
                }
            }
            if (sheet == null) {
                return purchases;
            }
        }
        
        Iterator<Row> rowIterator = sheet.iterator();
        Map<String, Integer> columnMap = new HashMap<>();
        
        // Read header row to build column mapping
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            columnMap = buildColumnMap(headerRow);
        }
        
        // Process data rows
        int processedRows = 0;
        int validPurchases = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            processedRows++;
            Purchase purchase = parseEsppRow(row, columnMap);
            if (purchase != null) {
                purchases.add(purchase);
                validPurchases++;
            }
        }
        
        return purchases;
    }
    
    /**
     * Parse ETRADE Benefit History Excel file
     */
    public static List<Purchase> parse(String filePath) {
        return parse(filePath, null);
    }
    
    /**
     * Parse ETRADE Benefit History Excel file with optional password
     */
    public static List<Purchase> parse(String filePath, String password) {
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            
            Workbook workbook = null;
            // Try XLSX first, then XLS as fallback
            try {
                workbook = new XSSFWorkbook(fis);
                Logger.log("Successfully opened as XLSX format");
            } catch (Exception e1) {
                try {
                    fis.close();
                    FileInputStream fis2 = new FileInputStream(filePath);
                    workbook = new HSSFWorkbook(fis2);
                    Logger.log("Successfully opened as XLS format");
                } catch (Exception e2) {
                    Logger.log("Failed to open as both XLSX and XLS formats");
                    Logger.log("XLSX error: " + e1.getMessage());
                    Logger.log("XLS error: " + e2.getMessage());
                    throw new RuntimeException("Unable to parse Excel file in either format", e2);
                }
            }
            
            if (workbook == null) {
                throw new RuntimeException("Failed to initialize workbook");
            }
            
            try {
                List<Purchase> purchases = new ArrayList<>();
                
                // Parse ESPP
                List<Purchase> esppPurchases = parseEspp(workbook);
                purchases.addAll(esppPurchases);
                
                // Parse RSU
                List<Purchase> rsuPurchases = parseRsu(workbook);
                purchases.addAll(rsuPurchases);
                
                Logger.log("Found %d ESPP purchases, %d RSU purchases", 
                    esppPurchases.size(), rsuPurchases.size());
                
                return purchases;
                
            } finally {
                workbook.close();
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file", e);
        }
    }
    
    /**
     * Helper method to get string value from cell
     */
    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
} 