package com.sefa.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * File utility functions
 * Converted from Python utils/file_utils.py
 */
public class FileUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Write object to JSON file
     */
    public static String writeToFile(String outputFolderAbsPath, String fileName, 
                                   Object obj, boolean override, boolean printPathToConsole) 
                                   throws IOException {
        Path outputPath = Paths.get(outputFolderAbsPath);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        Path finalFilePath = outputPath.resolve(fileName);
        
        if (Files.exists(finalFilePath) && !override) {
            throw new IllegalArgumentException(
                "Path " + finalFilePath + " already exists and force(-f) flag is not added to delete the path"
            );
        }
        
        try (FileWriter writer = new FileWriter(finalFilePath.toFile())) {
            objectMapper.writeValue(writer, obj);
            if (printPathToConsole) {
                printFilePath(finalFilePath.toString());
            }
        }
        
        return finalFilePath.toString();
    }
    
    /**
     * Write object to JSON file (without print to console)
     */
    public static String writeToFile(String outputFolderAbsPath, String fileName, 
                                   Object obj, boolean override) throws IOException {
        return writeToFile(outputFolderAbsPath, fileName, obj, override, false);
    }
    
    /**
     * Write CSV data to file
     */
    public static String writeCsvToFile(String outputFileAbsPath, String fileName,
                                      String[] headers, List<String[]> data,
                                      boolean override, boolean printPathToConsole) 
                                      throws IOException {
        Path outputPath = Paths.get(outputFileAbsPath);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        Path finalFilePath = outputPath.resolve(fileName);
        
        if (Files.exists(finalFilePath) && !override) {
            throw new IllegalArgumentException(
                "Path " + finalFilePath + " already exists and force(-f) flag is not added to delete the path"
            );
        }
        
        try (FileWriter fileWriter = new FileWriter(finalFilePath.toFile());
             CSVWriter csvWriter = new CSVWriter(fileWriter)) {
            
            // Write headers
            csvWriter.writeNext(headers);
            
            // Write data rows
            for (String[] row : data) {
                csvWriter.writeNext(row);
            }
            
            if (printPathToConsole) {
                printFilePath(finalFilePath.toString());
            }
        }
        
        return finalFilePath.toString();
    }
    
    /**
     * Write CSV data to file (without print to console)
     */
    public static String writeCsvToFile(String outputFileAbsPath, String fileName,
                                      String[] headers, List<String[]> data,
                                      boolean override) throws IOException {
        return writeCsvToFile(outputFileAbsPath, fileName, headers, data, override, false);
    }
    
    /**
     * Print file path to console
     */
    private static void printFilePath(String finalPath) {
        System.out.println("Output file created at " + finalPath);
    }
    
    /**
     * Check if file exists
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * Create directory if it doesn't exist
     */
    public static void createDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * Read text file content
     */
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
    
    /**
     * Read JSON file and convert to object
     */
    public static <T> T readJsonFile(String filePath, Class<T> valueType) throws IOException {
        return objectMapper.readValue(new File(filePath), valueType);
    }
} 