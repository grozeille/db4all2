package fr.grozeille.db4all.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

import tools.jackson.databind.JsonNode;

/**
 * Reads Excel tables.
 *
 * <p>DuckDB cannot read Excel files natively, so the selected area is first
 * materialized to a temporary CSV file with Apache POI, then queried through
 * {@code read_csv_auto}. Supported selection modes (mutually exclusive):</p>
 * <ul>
 *   <li>a whole sheet ({@code sheetName}, first sheet by default)</li>
 *   <li>a named range ({@code namedRange}, resolves its own sheet and area)</li>
 *   <li>a custom range ({@code explicitRange}, ex: {@code A1:D10} or {@code Sheet2!A1:D10})</li>
 * </ul>
 */
public final class TableExcelSource {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TableExcelSource() {
    }

    public record ExcelSelection(String sheetName, String namedRange, String explicitRange, boolean firstRowAsHeader) {
    }

    public record SheetInfo(String name, int index, boolean hidden, int rows) {
    }

    public record NamedRangeInfo(String name, String formula, String sheetName) {
    }

    public static String buildSourceSql() {
        return "SELECT * FROM read_csv_auto(?)";
    }

    public static ExcelSelection parseSelection(JsonNode configuration) {
        if (configuration == null || !configuration.isObject()) {
            throw new IllegalArgumentException("Table configuration is required.");
        }
        String path = trimToNull(configuration.path("path").asText(null));
        if (path != null && (path.contains("*") || path.contains("?") || path.contains("["))) {
            throw new IllegalArgumentException(
                    "Excel tables point to a single file, wildcards are not supported in path \"" + path + "\".");
        }
        String sheetName = trimToNull(configuration.path("sheetName").asText(null));
        String namedRange = trimToNull(configuration.path("namedRange").asText(null));
        String explicitRange = trimToNull(configuration.path("explicitRange").asText(null));
        if (namedRange != null && (sheetName != null || explicitRange != null)) {
            throw new IllegalArgumentException("namedRange cannot be combined with sheetName or explicitRange: "
                    + "a named range already designates its sheet and area.");
        }
        if (explicitRange != null) {
            parseArea(explicitRange, "Invalid explicit range \"" + explicitRange
                    + "\": expected something like \"A1:D10\" or \"Sheet2!A1:D10\".");
        }
        JsonNode headerNode = configuration.path("firstRowAsHeader");
        boolean firstRowAsHeader = headerNode.isMissingNode() || headerNode.isNull() || headerNode.asBoolean();
        return new ExcelSelection(sheetName, namedRange, explicitRange, firstRowAsHeader);
    }

    public static Path materializeToCsv(Path excelFile, ExcelSelection selection) {
        if (excelFile == null || !Files.isRegularFile(excelFile)) {
            throw new IllegalArgumentException("Excel file does not exist: " + excelFile);
        }
        try (Workbook workbook = openWorkbook(excelFile)) {
            ResolvedArea area = resolveArea(workbook, selection);
            List<List<String>> grid = readGrid(workbook, area);
            return writeCsv(grid, selection.firstRowAsHeader());
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Excel file \"" + excelFile.getFileName() + "\".", exception);
        }
    }

    public static List<SheetInfo> listSheets(Path excelFile) {
        try (Workbook workbook = openWorkbook(excelFile)) {
            List<SheetInfo> sheets = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                Sheet sheet = workbook.getSheetAt(index);
                boolean hidden = workbook.isSheetHidden(index) || workbook.isSheetVeryHidden(index);
                sheets.add(new SheetInfo(sheet.getSheetName(), index, hidden, sheet.getPhysicalNumberOfRows()));
            }
            return sheets;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Excel file \"" + excelFile.getFileName() + "\".", exception);
        }
    }

    public static List<NamedRangeInfo> listNamedRanges(Path excelFile) {
        try (Workbook workbook = openWorkbook(excelFile)) {
            List<NamedRangeInfo> ranges = new ArrayList<>();
            for (Name name : workbook.getAllNames()) {
                if (name.isFunctionName() || name.isDeleted() || name.getRefersToFormula() == null) {
                    continue;
                }
                AreaReference area;
                try {
                    area = new AreaReference(name.getRefersToFormula(), SpreadsheetVersion.EXCEL2007);
                } catch (RuntimeException exception) {
                    continue;
                }
                String sheetName = area.getFirstCell().getSheetName();
                if (sheetName == null) {
                    sheetName = name.getSheetName();
                }
                ranges.add(new NamedRangeInfo(name.getNameName(), name.getRefersToFormula(), sheetName));
            }
            ranges.sort(Comparator.comparing(NamedRangeInfo::name, String.CASE_INSENSITIVE_ORDER));
            return ranges;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Excel file \"" + excelFile.getFileName() + "\".", exception);
        }
    }

    private static ResolvedArea resolveArea(Workbook workbook, ExcelSelection selection) {
        if (selection.namedRange() != null) {
            return resolveNamedRange(workbook, selection.namedRange());
        }
        Sheet sheet = resolveSheet(workbook, selection.sheetName());
        if (selection.explicitRange() != null) {
            AreaReference area = parseArea(selection.explicitRange(), "Invalid explicit range \""
                    + selection.explicitRange() + "\": expected something like \"A1:D10\" or \"Sheet2!A1:D10\".");
            String areaSheetName = area.getFirstCell().getSheetName();
            if (areaSheetName != null) {
                sheet = resolveSheet(workbook, areaSheetName);
            }
            return fromAreaReference(sheet, area);
        }
        return usedRange(sheet);
    }

    private static ResolvedArea resolveNamedRange(Workbook workbook, String namedRange) {
        Name match = null;
        for (Name name : workbook.getAllNames()) {
            if (!name.getNameName().equalsIgnoreCase(namedRange)) {
                continue;
            }
            if (match == null || match.getSheetIndex() != -1) {
                match = name;
            }
        }
        if (match == null || match.getRefersToFormula() == null) {
            throw new IllegalArgumentException("Named range \"" + namedRange + "\" not found in workbook."
                    + availableNames(workbook.getAllNames().stream().map(Name::getNameName).toList()));
        }
        AreaReference area;
        try {
            area = new AreaReference(match.getRefersToFormula(), SpreadsheetVersion.EXCEL2007);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Named range \"" + namedRange + "\" does not designate a cell range: "
                    + match.getRefersToFormula());
        }
        String sheetName = area.getFirstCell().getSheetName();
        if (sheetName == null) {
            sheetName = match.getSheetName();
        }
        if (sheetName == null) {
            throw new IllegalArgumentException(
                    "Named range \"" + namedRange + "\" does not designate a sheet.");
        }
        return fromAreaReference(resolveSheet(workbook, sheetName), area);
    }

    private static Sheet resolveSheet(Workbook workbook, String sheetName) {
        if (sheetName != null) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                List<String> names = new ArrayList<>();
                for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                    names.add(workbook.getSheetName(index));
                }
                throw new IllegalArgumentException("Sheet \"" + sheetName + "\" not found in workbook."
                        + availableNames(names));
            }
            return sheet;
        }
        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException("Workbook contains no sheet.");
        }
        return workbook.getSheetAt(0);
    }

    private static ResolvedArea usedRange(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() == 0) {
            throw new IllegalArgumentException("Sheet \"" + sheet.getSheetName() + "\" is empty.");
        }
        int firstCol = Integer.MAX_VALUE;
        int lastCol = Integer.MIN_VALUE;
        for (Row row : sheet) {
            if (row == null || row.getFirstCellNum() < 0) {
                continue;
            }
            firstCol = Math.min(firstCol, row.getFirstCellNum());
            lastCol = Math.max(lastCol, row.getLastCellNum() - 1);
        }
        if (lastCol < firstCol) {
            throw new IllegalArgumentException("Sheet \"" + sheet.getSheetName() + "\" is empty.");
        }
        return new ResolvedArea(sheet, sheet.getFirstRowNum(), sheet.getLastRowNum(), firstCol, lastCol);
    }

    private static ResolvedArea fromAreaReference(Sheet sheet, AreaReference area) {
        CellReference first = area.getFirstCell();
        CellReference last = area.getLastCell();
        return new ResolvedArea(sheet,
                Math.min(first.getRow(), last.getRow()), Math.max(first.getRow(), last.getRow()),
                Math.min(first.getCol(), last.getCol()), Math.max(first.getCol(), last.getCol()));
    }

    private static List<List<String>> readGrid(Workbook workbook, ResolvedArea area) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<List<String>> grid = new ArrayList<>();
        for (int rowIndex = area.firstRow(); rowIndex <= area.lastRow(); rowIndex++) {
            Row row = area.sheet().getRow(rowIndex);
            List<String> values = new ArrayList<>();
            boolean blank = true;
            for (int colIndex = area.firstCol(); colIndex <= area.lastCol(); colIndex++) {
                String value = formatCell(row == null ? null : row.getCell(colIndex), evaluator);
                values.add(value);
                if (!value.isEmpty()) {
                    blank = false;
                }
            }
            if (!blank) {
                grid.add(values);
            }
        }
        return grid;
    }

    private static String formatCell(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue() ? "true" : "false";
            case NUMERIC -> formatNumeric(cell, cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            case FORMULA -> formatFormulaResult(cell, evaluator.evaluate(cell));
            case BLANK, ERROR, _NONE -> "";
        };
    }

    private static String formatFormulaResult(Cell cell, CellValue value) {
        if (value == null) {
            return "";
        }
        return switch (value.getCellType()) {
            case BOOLEAN -> value.getBooleanValue() ? "true" : "false";
            case NUMERIC -> formatNumeric(cell, value.getNumberValue());
            case STRING -> value.getStringValue();
            case FORMULA, BLANK, ERROR, _NONE -> "";
        };
    }

    private static String formatNumeric(Cell cell, double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        if (DateUtil.isCellDateFormatted(cell)) {
            return formatDateTime(cell);
        }
        if (value == Math.rint(value) && Math.abs(value) < 9.007199254740992E15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String formatDateTime(Cell cell) {
        LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
        if (dateTime.toLocalDate().equals(LocalDate.of(1899, 12, 30))) {
            return dateTime.toLocalTime().format(TIME_FORMAT);
        }
        if (dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return dateTime.toLocalDate().toString();
        }
        return dateTime.format(DATE_TIME_FORMAT);
    }

    private static Path writeCsv(List<List<String>> grid, boolean firstRowAsHeader) throws IOException {
        List<String> header;
        List<List<String>> data;
        int width = grid.stream().mapToInt(List::size).max().orElse(0);
        if (firstRowAsHeader && !grid.isEmpty()) {
            header = sanitizeHeader(grid.get(0));
            data = grid.subList(1, grid.size());
        } else {
            header = new ArrayList<>();
            for (int index = 0; index < width; index++) {
                header.add("column" + index);
            }
            data = grid;
        }
        Path csvFile = Files.createTempFile("db4all-excel-", ".csv");
        StringBuilder content = new StringBuilder();
        appendCsvRow(content, header, header.size());
        for (List<String> row : data) {
            appendCsvRow(content, row, header.size());
        }
        Files.writeString(csvFile, content.toString(), StandardCharsets.UTF_8);
        return csvFile;
    }

    private static void appendCsvRow(StringBuilder content, List<String> row, int width) {
        for (int index = 0; index < width; index++) {
            if (index > 0) {
                content.append(',');
            }
            String value = index < row.size() ? row.get(index) : "";
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                content.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                content.append(value);
            }
        }
        content.append('\n');
    }

    private static List<String> sanitizeHeader(List<String> raw) {
        List<String> header = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int index = 0; index < raw.size(); index++) {
            String base = raw.get(index).trim();
            if (base.isEmpty()) {
                base = "column" + index;
            }
            String candidate = base;
            int suffix = 1;
            while (used.contains(candidate.toLowerCase(Locale.ROOT))) {
                candidate = base + "_" + suffix++;
            }
            used.add(candidate.toLowerCase(Locale.ROOT));
            header.add(candidate);
        }
        return header;
    }

    private static Workbook openWorkbook(Path excelFile) {
        try {
            return WorkbookFactory.create(excelFile.toFile());
        } catch (Exception exception) {
            String reason = exception.getMessage() != null ? exception.getMessage() : exception.toString();
            throw new IllegalArgumentException(
                    "Unable to read Excel file \"" + excelFile.getFileName() + "\": " + reason, exception);
        }
    }

    private static AreaReference parseArea(String range, String errorMessage) {
        try {
            return new AreaReference(range, SpreadsheetVersion.EXCEL2007);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String availableNames(List<String> names) {
        if (names.isEmpty() || names.size() > 20) {
            return "";
        }
        return " Available: " + String.join(", ", names) + ".";
    }

    private record ResolvedArea(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
    }
}
