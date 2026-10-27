package fr.grozeille.db4all.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the DuckDB source SQL used to read JSON tables.
 *
 * <p>A JSON table points to a file (or file pattern) and to an optional JSON path
 * designating the array of objects to expose as rows. Supported path syntax:</p>
 * <ul>
 *   <li>empty or {@code $}: the document itself is an array of objects (or newline-delimited JSON)</li>
 *   <li>{@code $.field1.field2} or {@code field1.field2}: dot-separated object field navigation</li>
 *   <li>{@code $["field name"]}: bracket-quoted field for names containing dots or spaces</li>
 * </ul>
 * <p>Array indices (ex: {@code $[0]}) are not supported: the path must resolve to an array.</p>
 */
public final class TableJsonSource {

    private TableJsonSource() {
    }

    public static String buildSourceSql(String jsonPath) {
        List<String> segments = parseJsonPath(jsonPath);
        if (segments.isEmpty()) {
            return "SELECT * FROM read_json_auto(?)";
        }
        String arrayExpression = segments.stream()
                .map(TableJsonSource::quoteIdentifier)
                .collect(Collectors.joining("."));
        return "SELECT \"item\".* FROM (SELECT unnest(" + arrayExpression + ") AS \"item\" FROM read_json_auto(?))";
    }

    public static List<String> parseJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return List.of();
        }
        String input = jsonPath.trim();
        if ("$".equals(input)) {
            return List.of();
        }
        if (input.startsWith("$")) {
            input = input.substring(1);
        }
        List<String> segments = new ArrayList<>();
        int index = 0;
        while (index < input.length()) {
            char current = input.charAt(index);
            if (current == '[') {
                index = parseBracketSegment(input, index, segments, jsonPath);
            } else {
                if (current == '.') {
                    index++;
                } else if (index != 0) {
                    throw invalid(jsonPath, "expected '.' or '[' at position " + index + ".");
                }
                int start = index;
                while (index < input.length() && input.charAt(index) != '.' && input.charAt(index) != '[') {
                    index++;
                }
                String segment = input.substring(start, index).trim();
                if (segment.isEmpty()) {
                    throw invalid(jsonPath, "empty field name.");
                }
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            throw invalid(jsonPath, "no field specified.");
        }
        return List.copyOf(segments);
    }

    private static int parseBracketSegment(String input, int index, List<String> segments, String jsonPath) {
        int cursor = index + 1;
        if (cursor >= input.length()) {
            throw invalid(jsonPath, "unclosed '['.");
        }
        char quote = input.charAt(cursor);
        if (quote != '"' && quote != '\'') {
            throw invalid(jsonPath, "only quoted field names like [\"field\"] are supported, array indices are not supported.");
        }
        cursor++;
        StringBuilder segment = new StringBuilder();
        boolean closed = false;
        while (cursor < input.length()) {
            char current = input.charAt(cursor);
            if (current == '\\' && cursor + 1 < input.length() && input.charAt(cursor + 1) == quote) {
                segment.append(quote);
                cursor += 2;
            } else if (current == quote) {
                cursor++;
                closed = true;
                break;
            } else {
                segment.append(current);
                cursor++;
            }
        }
        if (!closed) {
            throw invalid(jsonPath, "unclosed quoted field name.");
        }
        if (cursor >= input.length() || input.charAt(cursor) != ']') {
            throw invalid(jsonPath, "expected ']' after the quoted field name.");
        }
        if (segment.toString().trim().isEmpty()) {
            throw invalid(jsonPath, "empty field name.");
        }
        segments.add(segment.toString());
        return cursor + 1;
    }

    private static IllegalArgumentException invalid(String jsonPath, String reason) {
        return new IllegalArgumentException("Invalid JSON path \"" + jsonPath + "\": " + reason
                + " Expected something like \"$.data.items\".");
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
