# SeFA Java
Java implementation to generate Indian ITR schedule FA under section A3 automatically

## Requirements
- Java 11 or higher
- Maven 3.6 or higher
- Apache POI for Excel processing
- CSV files: `adobe_price_history.csv` and `usd_inr_price_history.csv`

## Build Instructions

### 1. Compile and Package
```bash
mvn clean package
```

This will create an executable JAR file: `target/sefa-java-1.0.0.jar`

### 2. Run the Application
```bash
java -jar target/sefa-java-1.0.0.jar -i "/path/to/BenefitHistory.xlsx" -ay 2023
```

## Command Line Options

```
usage: java -jar sefa-java.jar
SeFA Java - Indian ITR schedule FA under section A3 generator
 -ay,--assessment-year <ASSESSMENT_YEAR>   Current year of assessment year. For AY 2019-2020, input will be 2019
 -cal,--calendar-mode <CALENDAR_MODE>      Specify the calendar period for consideration (calendar|financial), default = calendar
 -h,--help                                 Show this help message
 -i,--input <INPUT_EXCEL_FILE>             Specify the absolute path for input benefit history(BenefitHistory.xlsx) Excel file
 -m,--source-mode <SOURCE_MODE>            Specify the source mode, default = etrade_benefit_history
 -o,--output <OUTPUT_FOLDER>               Specify the absolute path of output folder for JSON data, default = <current_dir>/output
 -v,--verbose                              Enable the debug logs

Example usage:
java -jar sefa-java.jar -i "/path/to/BenefitHistory.xlsx" -ay 2023

Note: This tool requires historic_data/adobe_price_history.csv and historic_data/usd_inr_price_history.csv files.
```

## Key Features Converted from Python

### ✅ **Completed Conversions:**

1. **Models**: All Python dataclasses converted to Java POJOs
   - `Organization` - Entity information
   - `Price` - Monetary values with currency
   - `DateObj` - Date handling with multiple formats
   - `Purchase` - Stock purchase transactions
   - `FAA3` - ITR Form A3 entries

2. **Utilities**: Core functionality preserved
   - `DateUtils` - Date parsing and formatting
   - `Logger` - Logging with debug support
   - `FileUtils` - JSON and CSV file operations
   - `TickerMapping` - Organization and currency mappings
   - `ShareDataUtils` - Stock price and exchange rate processing

3. **Parsers**: Excel and data processing
   - `EtradeBenefitHistoryParser` - ETRADE Excel file parsing
   - `FAA3Parser` - ITR schedule generation

4. **Main Application**: Command-line interface
   - `SeFA` - Main application with CLI argument parsing

### 🔄 **Key Conversions Made:**

- **pandas** → **Apache POI + OpenCSV**: Excel and CSV processing
- **Python typing** → **Java generics**: Type safety
- **Python dataclasses** → **Java POJOs**: Object modeling
- **Python argparse** → **Apache Commons CLI**: Command line parsing
- **Python json** → **Jackson**: JSON serialization
- **Python datetime** → **Java Time API**: Date/time handling

### 📊 **Output Format:**

The Java version generates the exact same output format as requested:

| Country | Name of Entity | Address of Entity | Zip Code | Nature of Entity | Date of Acquisition | Initial Investment | Peak Investment | Closing Balance | Total Gross Amount | Sales Proceeds or Redemption during the year |
|---------|----------------|-------------------|----------|------------------|--------------------|--------------------|-----------------|-----------------|--------------------|--------------------------------------------- |
| 2 - United States | Adobe Incorporation | 345 Park Avenue San Jose, CA | 95110 | Listed | 15-Jun-2024 | 249896 | 492233 | 375742 | 492233 | 0 |

## Dependencies

The Java version uses these libraries to replace Python dependencies:

- **Apache POI** - Excel file processing (replaces pandas + openpyxl)
- **OpenCSV** - CSV file processing (replaces pandas CSV functions)
- **Jackson** - JSON processing (replaces Python json)
- **Apache Commons CLI** - Command line parsing (replaces argparse)
- **SLF4J + Logback** - Logging (replaces Python logging)

## Development

### Project Structure
```
src/
├── main/java/com/sefa/
│   ├── models/           # Data models (POJOs)
│   ├── utils/            # Utility classes
│   ├── parsers/          # Data parsers
│   │   ├── etrade/       # ETRADE-specific parsers
│   │   └── itr/          # ITR-specific parsers
│   └── SeFA.java         # Main application
├── main/resources/       # Configuration files
└── test/java/com/sefa/   # Unit tests
```

### Build Configuration
- **Maven** project with Java 11 target
- **Executable JAR** with all dependencies included
- **Main class**: `com.sefa.SeFA`

## Migration Notes

This Java version maintains complete functional compatibility with the Python version while providing:

- **Better Performance**: Native Java performance, especially for large datasets
- **No Python Dependencies**: Eliminates pandas, openpyxl installation issues
- **Cross-Platform**: Runs anywhere Java is installed
- **Enterprise Ready**: Standard Java tooling and deployment options
- **Type Safety**: Compile-time type checking reduces runtime errors

The same CSV data files (`adobe_price_history.csv`, `usd_inr_price_history.csv`) are used, ensuring identical calculations and results. 