package com.sefa.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Excel debugging utility to help identify correct column mappings
 */
public class ExcelDebugger {
    
    public static void debugExcelFile(String filePath) {
        System.out.println("=== Excel File Debug Information ===");
        System.out.println("File: " + filePath);
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            // List all sheets
            System.out.println("\nSheets found:");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("  - " + sheet.getSheetName() + " (" + sheet.getLastRowNum() + " rows)");
            }
            
            // Debug ESPP sheet
            debugSheet(workbook, "ESPP");
            
            // Debug RSU sheet
            debugSheet(workbook, "Restricted Stock");
            
        } catch (IOException e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
        }
    }
    
    private static void debugSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            System.out.println("\n" + sheetName + " sheet not found");
            return;
        }
        
        System.out.println("\n=== " + sheetName + " Sheet ===");
        System.out.println("Total rows: " + (sheet.getLastRowNum() + 1));
        
        // Show header row
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            System.out.println("\nHeader row (Row 0):");
            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String value = getCellStringValue(cell);
                System.out.println("  Column " + cell.getColumnIndex() + ": \"" + value + "\"");
            }
        }
        
        // Show first few data rows
        System.out.println("\nFirst 3 data rows:");
        for (int rowNum = 1; rowNum <= Math.min(3, sheet.getLastRowNum()); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                System.out.println("Row " + rowNum + ":");
                for (int colNum = 0; colNum < Math.min(10, row.getLastCellNum()); colNum++) {
                    Cell cell = row.getCell(colNum);
                    String value = getCellStringValue(cell);
                    if (!value.isEmpty()) {
                        System.out.println("  Col " + colNum + ": \"" + value + "\"");
                    }
                }
                System.out.println();
            }
        }
    }
    
    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ExcelDebugger <excel-file-path>");
            return;
        }
        
        debugExcelFile(args[0]);
    }
} 