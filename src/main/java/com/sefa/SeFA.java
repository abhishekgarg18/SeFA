package com.sefa;

import com.sefa.models.Purchase;
import com.sefa.parsers.etrade.EtradeBenefitHistoryParser;
import com.sefa.parsers.itr.FAA3Parser;
import com.sefa.utils.Logger;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main SeFA Application
 * Converted from Python run.py
 */
public class SeFA {
    
    // Argument defaults
    private static final String DEFAULT_OUTPUT_FOLDER_NAME = "output";
    private static final String DEFAULT_SOURCE_MODE = "etrade_benefit_history";
    private static final String DEFAULT_CALENDAR_MODE = "calendar";
    
    public static void main(String[] args) {
        try {
            runApplication(args);
            Logger.log("On your left!");
        } catch (Exception e) {
            Logger.error("Application failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void runApplication(String[] args) throws IOException, ParseException {
        // Get default output folder
        String currentDir = System.getProperty("user.dir");
        String defaultOutputFolderAbsPath = Paths.get(currentDir, DEFAULT_OUTPUT_FOLDER_NAME).toString();
        
        // Setup command line options
        Options options = new Options();
        
        options.addOption(Option.builder("o")
            .longOpt("output")
            .hasArg()
            .argName("OUTPUT_FOLDER")
            .desc("Specify the absolute path of output folder for JSON data, default = " + defaultOutputFolderAbsPath)
            .build());
        
        options.addOption(Option.builder("i")
            .longOpt("input")
            .hasArg()
            .argName("INPUT_EXCEL_FILE")
            .desc("Specify the absolute path for input benefit history(BenefitHistory.xlsx) Excel file")
            .required()
            .build());
        
        options.addOption(Option.builder("m")
            .longOpt("source-mode")
            .hasArg()
            .argName("SOURCE_MODE")
            .desc("Specify the source mode, default = " + DEFAULT_SOURCE_MODE)
            .build());
        
        options.addOption(Option.builder("ay")
            .longOpt("assessment-year")
            .hasArg()
            .argName("ASSESSMENT_YEAR")
            .desc("Assessment year (optional - if not provided, processes all data)")
            .build());
        
        options.addOption(Option.builder("cal")
            .longOpt("calendar-mode")
            .hasArg()
            .argName("CALENDAR_MODE")
            .desc("Calendar mode (optional - default: calendar)")
            .build());
        
        options.addOption(Option.builder("v")
            .longOpt("verbose")
            .desc("Enable the debug logs")
            .build());
        
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Show this help message")
            .build());
        
        // Parse command line arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(options);
            throw e;
        }
        
        // Show help if requested
        if (cmd.hasOption("h")) {
            printHelp(options);
            return;
        }
        
        // Extract arguments
        String outputFolder = cmd.getOptionValue("o", defaultOutputFolderAbsPath);
        String inputExcelFile = cmd.getOptionValue("i");
        String sourceMode = cmd.getOptionValue("m", DEFAULT_SOURCE_MODE);
        String calendarMode = cmd.getOptionValue("cal", DEFAULT_CALENDAR_MODE);
        int assessmentYear = -1; // Default to -1 to indicate no specific year
        
        if (cmd.hasOption("ay")) {
            try {
                assessmentYear = Integer.parseInt(cmd.getOptionValue("ay"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Assessment year must be a valid integer");
            }
        }
        
        boolean debug = cmd.hasOption("v");
        
        // Set debug mode
        Logger.setDebug(debug);
        EtradeBenefitHistoryParser.setDebug(debug);
        
        // Validate inputs
        if (!calendarMode.equals("calendar") && !calendarMode.equals("financial")) {
            throw new IllegalArgumentException("Calendar mode must be 'calendar' or 'financial'");
        }
        
        // Log configuration
        Logger.log("SeFA Java Application Starting...");
        Logger.log("Input Excel File: %s", inputExcelFile);
        Logger.log("Output Folder: %s", outputFolder);
        Logger.log("Source Mode: %s", sourceMode);
        Logger.log("Calendar Mode: %s", calendarMode);
        if (assessmentYear != -1) {
            Logger.log("Assessment Year: %d", assessmentYear);
        } else {
            Logger.log("Processing all data (no assessment year filter)");
        }
        Logger.log("Debug Mode: %s", debug);
        
        // Parse purchases based on source mode
        List<Purchase> purchases;
        
        if ("etrade_benefit_history".equals(sourceMode)) {
            purchases = EtradeBenefitHistoryParser.parse(inputExcelFile);
        } else {
            throw new UnsupportedOperationException("Source mode '" + sourceMode + "' is not yet implemented in Java version");
        }
        
        if (purchases.isEmpty()) {
            Logger.warn("No purchases found in the input file");
            return;
        }
        
        // Process FAA3 entries
        Logger.log("Processing FAA3 entries...");
        FAA3Parser.parse(calendarMode, purchases, assessmentYear, outputFolder);
        
        Logger.log("Processing completed successfully!");
    }
    
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar sefa-java.jar", 
            "SeFA Java - Indian ITR schedule FA under section A3 generator", 
            options, 
            "\nExample usage:\n" +
            "java -jar sefa-java.jar -i \"/path/to/BenefitHistory.xlsx\"\n" +
            "java -jar sefa-java.jar -i \"/path/to/BenefitHistory.xlsx\" -ay 2023\n" +
            "\nNote: This tool requires historic_data/adobe_price_history.csv and historic_data/usd_inr_price_history.csv files.",
            true);
    }
} 