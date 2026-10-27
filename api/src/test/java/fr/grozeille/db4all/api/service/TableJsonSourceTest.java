package fr.grozeille.db4all.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableJsonSourceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldParseRootPathsAsEmpty() {
        assertEquals(List.of(), TableJsonSource.parseJsonPath(null));
        assertEquals(List.of(), TableJsonSource.parseJsonPath(""));
        assertEquals(List.of(), TableJsonSource.parseJsonPath("   "));
        assertEquals(List.of(), TableJsonSource.parseJsonPath("$"));
    }

    @Test
    void shouldParseDotNotationWithOrWithoutDollar() {
        assertEquals(List.of("items"), TableJsonSource.parseJsonPath("$.items"));
        assertEquals(List.of("data", "rows"), TableJsonSource.parseJsonPath("$.data.rows"));
        assertEquals(List.of("data", "rows"), TableJsonSource.parseJsonPath("data.rows"));
        assertEquals(List.of("items"), TableJsonSource.parseJsonPath("  $.items  "));
    }

    @Test
    void shouldParseBracketQuotedSegments() {
        assertEquals(List.of("my field"), TableJsonSource.parseJsonPath("$[\"my field\"]"));
        assertEquals(List.of("a", "b.c"), TableJsonSource.parseJsonPath("$.a['b.c']"));
        assertEquals(List.of("a", "b.c"), TableJsonSource.parseJsonPath("$['a']['b.c']"));
    }

    @Test
    void shouldRejectInvalidPaths() {
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$."));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$.a..b"));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("."));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$[0]"));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$.a[0]"));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$[\"a\""));
        assertThrows(IllegalArgumentException.class, () -> TableJsonSource.parseJsonPath("$[\"a\"]x"));
    }

    @Test
    void shouldBuildDirectReadForRootPath() {
        assertEquals("SELECT * FROM read_json_auto(?)", TableJsonSource.buildSourceSql(null));
        assertEquals("SELECT * FROM read_json_auto(?)", TableJsonSource.buildSourceSql("$"));
    }

    @Test
    void shouldBuildUnnestReadForNestedPath() {
        assertEquals(
                "SELECT \"item\".* FROM (SELECT unnest(\"data\".\"rows\") AS \"item\" FROM read_json_auto(?))",
                TableJsonSource.buildSourceSql("$.data.rows"));
    }

    @Test
    void shouldEscapeQuotesInFieldNames() {
        assertEquals(
                "SELECT \"item\".* FROM (SELECT unnest(\"a\"\"b\") AS \"item\" FROM read_json_auto(?))",
                TableJsonSource.buildSourceSql("$.a\"b"));
    }

    @Test
    void shouldReadNestedArrayThroughDuckDb() throws Exception {
        Path file = writeFile("nested.json",
                "{\"metadata\": {\"source\": \"demo\"}, \"items\": ["
                        + "{\"id\": 1, \"name\": \"camille\", \"city\": \"Paris\"}, "
                        + "{\"id\": 2, \"name\": \"raphael\", \"city\": \"Lyon\"}]}");

        QueryResult result = execute(TableJsonSource.buildSourceSql("$.items"), file);

        assertEquals(List.of("id", "name", "city"), result.columns());
        assertEquals(2, result.rows().size());
        assertEquals(List.of(1L, "camille", "Paris"), result.rows().get(0));
        assertEquals(List.of(2L, "raphael", "Lyon"), result.rows().get(1));
    }

    @Test
    void shouldReadDeepArrayThroughDuckDb() throws Exception {
        Path file = writeFile("deep.json", "{\"data\": {\"rows\": [{\"a\": 10}, {\"a\": 20}], \"total\": 2}}");

        QueryResult result = execute(TableJsonSource.buildSourceSql("data.rows"), file);

        assertEquals(List.of("a"), result.columns());
        assertEquals(2, result.rows().size());
    }

    @Test
    void shouldReadRootArrayAndNewlineDelimitedJsonThroughDuckDb() throws Exception {
        Path arrayFile = writeFile("flat.json", "[{\"id\": 1}, {\"id\": 2}]");
        Path ndJsonFile = writeFile("lines.jsonl", "{\"id\": 1}\n{\"id\": 2}\n");

        assertEquals(2, execute(TableJsonSource.buildSourceSql(null), arrayFile).rows().size());
        assertEquals(2, execute(TableJsonSource.buildSourceSql("$"), arrayFile).rows().size());
        QueryResult ndJson = execute(TableJsonSource.buildSourceSql(null), ndJsonFile);
        assertEquals(List.of("id"), ndJson.columns());
        assertEquals(2, ndJson.rows().size());
    }

    @Test
    void shouldFailWhenPathDoesNotResolveToAnArrayOfObjects() throws Exception {
        Path file = writeFile("missing.json", "{\"items\": [{\"id\": 1}]}");

        assertThrows(Exception.class, () -> execute(TableJsonSource.buildSourceSql("$.unknown"), file));
    }

    private Path writeFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private QueryResult execute(String sql, Path file) throws Exception {
        assertTrue(sql.contains("?"), "generated SQL must keep the file path as a bound parameter");
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, file.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= metaData.getColumnCount(); index++) {
                    columns.add(metaData.getColumnLabel(index));
                }
                List<List<Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        row.add(resultSet.getObject(index));
                    }
                    rows.add(row);
                }
                return new QueryResult(columns, rows);
            }
        }
    }

    private record QueryResult(List<String> columns, List<List<Object>> rows) {
    }
}
