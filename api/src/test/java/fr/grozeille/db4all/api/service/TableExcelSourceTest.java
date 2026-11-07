package fr.grozeille.db4all.api.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableExcelSourceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    private Path tempDir;

    @Test
    void shouldParseSelectionDefaults() {
        TableExcelSource.ExcelSelection selection = TableExcelSource.parseSelection(config());

        assertNull(selection.sheetName());
        assertNull(selection.namedRange());
        assertNull(selection.explicitRange());
        assertTrue(selection.firstRowAsHeader());
    }

    @Test
    void shouldParseEachSelectionMode() {
        TableExcelSource.ExcelSelection sheet = TableExcelSource.parseSelection(config()
                .put("sheetName", " Orders ")
                .put("firstRowAsHeader", false));
        assertEquals("Orders", sheet.sheetName());
        assertFalse(sheet.firstRowAsHeader());

        TableExcelSource.ExcelSelection named = TableExcelSource.parseSelection(config()
                .put("namedRange", "TopCustomers"));
        assertEquals("TopCustomers", named.namedRange());

        TableExcelSource.ExcelSelection custom = TableExcelSource.parseSelection(config()
                .put("sheetName", "Orders")
                .put("explicitRange", "B1:C10"));
        assertEquals("B1:C10", custom.explicitRange());
    }

    @Test
    void shouldRejectWildcardPaths() {
        assertThrows(IllegalArgumentException.class,
                () -> TableExcelSource.parseSelection(config().put("path", "*.xlsx")));
        assertThrows(IllegalArgumentException.class,
                () -> TableExcelSource.parseSelection(config().put("path", "data?.xlsx")));
    }

    @Test
    void shouldRejectAmbiguousSelections() {
        assertThrows(IllegalArgumentException.class, () -> TableExcelSource.parseSelection(config()
                .put("namedRange", "TopCustomers")
                .put("sheetName", "Orders")));
        assertThrows(IllegalArgumentException.class, () -> TableExcelSource.parseSelection(config()
                .put("namedRange", "TopCustomers")
                .put("explicitRange", "A1:B2")));
    }

    @Test
    void shouldRejectInvalidExplicitRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> TableExcelSource.parseSelection(config().put("explicitRange", "not a range")));
        assertThrows(IllegalArgumentException.class,
                () -> TableExcelSource.parseSelection(config().put("explicitRange", "A:B:C")));
    }

    @Test
    void shouldReadWholeSheetThroughDuckDb() throws Exception {
        Path file = writeWorkbook("orders.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Orders");
            row(sheet, 0, "id", "name", "amount");
            row(sheet, 1, 1, "camille", 12.5);
            row(sheet, 2, 2, "raphael", 7);
        });

        QueryResult result = execute(file, TableExcelSource.parseSelection(config()));

        assertEquals(List.of("id", "name", "amount"), result.columns());
        assertEquals(2, result.rows().size());
        assertEquals(List.of(1L, "camille", 12.5), result.rows().get(0));
        assertEquals(List.of(2L, "raphael", 7.0), result.rows().get(1));
    }

    @Test
    void shouldDefaultToFirstSheetAndSupportSheetSelection() throws Exception {
        Path file = writeWorkbook("sheets.xlsx", workbook -> {
            Sheet first = workbook.createSheet("First");
            row(first, 0, "a");
            row(first, 1, 1);
            Sheet second = workbook.createSheet("Second");
            row(second, 0, "b");
            row(second, 1, 2);
        });

        assertEquals(List.of("a"), execute(file, TableExcelSource.parseSelection(config())).columns());

        TableExcelSource.ExcelSelection selection =
                TableExcelSource.parseSelection(config().put("sheetName", "Second"));
        QueryResult result = execute(file, selection);
        assertEquals(List.of("b"), result.columns());
        assertEquals(List.of(List.of(2L)), result.rows());
    }

    @Test
    void shouldReadNamedRange() throws Exception {
        Path file = writeWorkbook("named.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Orders");
            row(sheet, 0, "id", "name");
            row(sheet, 1, 1, "camille");
            row(sheet, 2, 2, "raphael");
            Name name = workbook.createName();
            name.setNameName("TopCustomers");
            name.setRefersToFormula("Orders!$A$1:$B$3");
        });

        TableExcelSource.ExcelSelection selection =
                TableExcelSource.parseSelection(config().put("namedRange", "topcustomers"));
        QueryResult result = execute(file, selection);

        assertEquals(List.of("id", "name"), result.columns());
        assertEquals(2, result.rows().size());
    }

    @Test
    void shouldReadExplicitRangeWithOrWithoutSheetQualifier() throws Exception {
        Path file = writeWorkbook("range.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Orders");
            row(sheet, 0, "id", "name", "city");
            row(sheet, 1, 1, "camille", "Paris");
            row(sheet, 2, 2, "raphael", "Lyon");
        });

        TableExcelSource.ExcelSelection relative =
                TableExcelSource.parseSelection(config().put("sheetName", "Orders").put("explicitRange", "B1:C3"));
        assertEquals(List.of("name", "city"), execute(file, relative).columns());

        TableExcelSource.ExcelSelection qualified =
                TableExcelSource.parseSelection(config().put("explicitRange", "Orders!B1:C3"));
        QueryResult result = execute(file, qualified);
        assertEquals(List.of("name", "city"), result.columns());
        assertEquals(List.of(List.of("camille", "Paris"), List.of("raphael", "Lyon")), result.rows());
    }

    @Test
    void shouldGenerateColumnNamesWithoutHeader() throws Exception {
        Path file = writeWorkbook("no-header.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Data");
            row(sheet, 0, 1, "camille");
            row(sheet, 1, 2, "raphael");
        });

        TableExcelSource.ExcelSelection selection =
                TableExcelSource.parseSelection(config().put("firstRowAsHeader", false));
        QueryResult result = execute(file, selection);

        assertEquals(List.of("column0", "column1"), result.columns());
        assertEquals(2, result.rows().size());
    }

    @Test
    void shouldFormatBooleansDatesFormulasAndSpecialCharacters() throws Exception {
        Path file = writeWorkbook("types.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Data");
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            row(sheet, 0, "active", "since", "total", "label");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(true);
            Cell dateCell = data.createCell(1);
            dateCell.setCellValue(LocalDateTime.of(2024, 5, 1, 0, 0));
            dateCell.setCellStyle(dateStyle);
            data.createCell(2).setCellFormula("40+2");
            data.createCell(3).setCellValue("a, \"quoted\" value");
        });

        QueryResult result = execute(file, TableExcelSource.parseSelection(config()));

        assertEquals(List.of("active", "since", "total", "label"), result.columns());
        assertEquals(1, result.rows().size());
        List<Object> values = result.rows().get(0);
        assertEquals(true, values.get(0));
        assertEquals("2024-05-01", values.get(1).toString());
        assertEquals(42L, ((Number) values.get(2)).longValue());
        assertEquals("a, \"quoted\" value", values.get(3));
    }

    @Test
    void shouldDedupeBlankAndDuplicateHeaders() throws Exception {
        Path file = writeWorkbook("headers.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Data");
            row(sheet, 0, "name", "", "name");
            row(sheet, 1, "camille", "x", "y");
        });

        QueryResult result = execute(file, TableExcelSource.parseSelection(config()));

        assertEquals(List.of("name", "column1", "name_1"), result.columns());
        assertEquals(1, result.rows().size());
    }

    @Test
    void shouldFailOnUnknownSheetNamedRangeOrEmptySheet() throws Exception {
        Path file = writeWorkbook("errors.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Orders");
            row(sheet, 0, "id");
            row(sheet, 1, 1);
            workbook.createSheet("Empty");
        });

        assertThrows(IllegalArgumentException.class,
                () -> execute(file, TableExcelSource.parseSelection(config().put("sheetName", "Missing"))));
        assertThrows(IllegalArgumentException.class,
                () -> execute(file, TableExcelSource.parseSelection(config().put("namedRange", "Missing"))));
        assertThrows(IllegalArgumentException.class,
                () -> execute(file, TableExcelSource.parseSelection(config().put("sheetName", "Empty"))));
    }

    @Test
    void shouldListSheetsAndNamedRanges() throws Exception {
        Path file = writeWorkbook("scan.xlsx", workbook -> {
            Sheet orders = workbook.createSheet("Orders");
            row(orders, 0, "id");
            row(orders, 1, 1);
            workbook.createSheet("Archive");
            Name range = workbook.createName();
            range.setNameName("TopCustomers");
            range.setRefersToFormula("Orders!$A$1:$A$2");
            Name constant = workbook.createName();
            constant.setNameName("TaxRate");
            constant.setRefersToFormula("0.2");
        });

        List<TableExcelSource.SheetInfo> sheets = TableExcelSource.listSheets(file);
        assertEquals(List.of("Orders", "Archive"), sheets.stream().map(TableExcelSource.SheetInfo::name).toList());
        assertEquals(0, sheets.get(0).index());
        assertFalse(sheets.get(0).hidden());

        List<TableExcelSource.NamedRangeInfo> ranges = TableExcelSource.listNamedRanges(file);
        assertEquals(1, ranges.size());
        assertEquals("TopCustomers", ranges.get(0).name());
        assertEquals("Orders!$A$1:$A$2", ranges.get(0).formula());
        assertEquals("Orders", ranges.get(0).sheetName());
    }

    @Test
    void shouldSupportConcurrentReadsWithoutTouchingTheSourceFile() throws Exception {
        Path file = writeWorkbook("concurrent.xlsx", workbook -> {
            Sheet sheet = workbook.createSheet("Data");
            row(sheet, 0, "id", "name");
            row(sheet, 1, 1, "camille");
        });
        byte[] before = Files.readAllBytes(file);

        int threads = 8;
        int iterations = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int task = 0; task < threads; task++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        assertEquals(1, TableExcelSource.listSheets(file).size());
                        Path csv = TableExcelSource.materializeToCsv(file, TableExcelSource.parseSelection(config()));
                        Files.deleteIfExists(csv);
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertTrue(Arrays.equals(before, Files.readAllBytes(file)), "reads must leave the source workbook untouched");
    }

    @Test
    void shouldRejectNonExcelFiles() throws Exception {
        Path file = tempDir.resolve("notes.txt");
        Files.writeString(file, "not a workbook");

        assertThrows(IllegalArgumentException.class,
                () -> TableExcelSource.materializeToCsv(file, TableExcelSource.parseSelection(config())));
        assertThrows(IllegalArgumentException.class, () -> TableExcelSource.listSheets(file));
        assertThrows(IllegalArgumentException.class, () -> TableExcelSource.listNamedRanges(file));
    }

    private ObjectNode config() {
        return OBJECT_MAPPER.createObjectNode().put("path", "orders.xlsx");
    }

    private Path writeWorkbook(String name, Consumer<XSSFWorkbook> filler) throws Exception {
        Path file = tempDir.resolve(name);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            filler.accept(workbook);
            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }
        return file;
    }

    private void row(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index);
            switch (values[index]) {
                case Integer number -> cell.setCellValue(number);
                case Double number -> cell.setCellValue(number);
                case String text -> cell.setCellValue(text);
                case null, default -> cell.setBlank();
            }
        }
    }

    private QueryResult execute(Path file, TableExcelSource.ExcelSelection selection) throws Exception {
        String sql = TableExcelSource.buildSourceSql();
        assertTrue(sql.contains("?"), "generated SQL must keep the file path as a bound parameter");
        Path csvFile = TableExcelSource.materializeToCsv(file, selection);
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, csvFile.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= metaData.getColumnCount(); index++) {
                    columns.add(metaData.getColumnLabel(index));
                }
                List<List<Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    List<Object> values = new ArrayList<>();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        values.add(resultSet.getObject(index));
                    }
                    rows.add(values);
                }
                return new QueryResult(columns, rows);
            }
        } finally {
            Files.deleteIfExists(csvFile);
        }
    }

    private record QueryResult(List<String> columns, List<List<Object>> rows) {
    }
}
