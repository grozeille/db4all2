package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.dto.AggregationFunctionDto;
import fr.grozeille.db4all.api.dto.AggregationRequestDto;
import fr.grozeille.db4all.api.dto.FilterConditionDto;
import fr.grozeille.db4all.api.dto.FilterGroupDto;
import fr.grozeille.db4all.api.dto.SelectedColumnDto;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.exceptions.InvalidQueryException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TableQuerySqlCompiler {

    public QueryPlan compile(String sourceSql,
                             List<Object> sourceParameters,
                             List<SchemaColumn> schema,
                             TableQueryRequest request,
                             boolean applyPagination) {
        TableQueryRequest effectiveRequest = request == null ? TableQueryRequest.builder().build() : request;
        Map<String, SchemaColumn> schemaByName = indexSchema(schema);

        StringBuilder sql = new StringBuilder("SELECT ");
        List<Object> parameters = new ArrayList<>(sourceParameters);
        List<String> projectedColumns;

        if (Boolean.TRUE.equals(effectiveRequest.getDistinct()) && effectiveRequest.getAggregations() != null) {
            throw new InvalidQueryException("distinct cannot be combined with aggregations.");
        }

        if (effectiveRequest.getAggregations() != null) {
            if (!CollectionUtils.isEmpty(effectiveRequest.getSelectedColumns())) {
                throw new InvalidQueryException("selectedColumns cannot be combined with aggregations.");
            }
            projectedColumns = appendAggregationSelect(sql, effectiveRequest.getAggregations(), schemaByName);
        } else {
            if (Boolean.TRUE.equals(effectiveRequest.getDistinct())) {
                sql.append("DISTINCT ");
            }
            projectedColumns = appendProjection(sql, effectiveRequest.getSelectedColumns(), schemaByName, schema);
        }

        sql.append(" FROM (").append(sourceSql).append(") source_data");

        String whereClause = compileWhereClause(effectiveRequest.getFilters(), schemaByName, parameters);
        if (StringUtils.hasText(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }

        if (effectiveRequest.getAggregations() != null && !CollectionUtils.isEmpty(effectiveRequest.getAggregations().getGroupedBy())) {
            sql.append(" GROUP BY ");
            sql.append(effectiveRequest.getAggregations().getGroupedBy().stream()
                    .map(column -> quoteIdentifier(resolveColumn(column, schemaByName).name()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElseThrow());
        }

        if (applyPagination) {
            int size = effectiveRequest.getSize() == null ? 100 : effectiveRequest.getSize();
            int page = effectiveRequest.getPage() == null ? 0 : effectiveRequest.getPage();
            if (size < 1 || size > 1000) {
                throw new InvalidQueryException("size must be between 1 and 1000.");
            }
            if (page < 0) {
                throw new InvalidQueryException("page must be greater than or equal to 0.");
            }
            sql.append(" LIMIT ? OFFSET ?");
            parameters.add(size);
            parameters.add(page * size);
        }

        return new QueryPlan(sql.toString(), List.copyOf(parameters), projectedColumns);
    }

    private Map<String, SchemaColumn> indexSchema(List<SchemaColumn> schema) {
        Map<String, SchemaColumn> index = new LinkedHashMap<>();
        for (SchemaColumn column : schema) {
            index.put(column.name().toLowerCase(Locale.ROOT), column);
        }
        return index;
    }

    private List<String> appendProjection(StringBuilder sql,
                                          List<SelectedColumnDto> selectedColumns,
                                          Map<String, SchemaColumn> schemaByName,
                                          List<SchemaColumn> schema) {
        List<String> projectedColumns = new ArrayList<>();
        List<String> fragments = new ArrayList<>();
        if (CollectionUtils.isEmpty(selectedColumns)) {
            for (SchemaColumn column : schema) {
                fragments.add(quoteIdentifier(column.name()));
                projectedColumns.add(column.name());
            }
        } else {
            for (SelectedColumnDto selectedColumn : selectedColumns) {
                SchemaColumn column = resolveColumn(selectedColumn.getColumn(), schemaByName);
                String fragment = quoteIdentifier(column.name());
                String outputName = column.name();
                if (StringUtils.hasText(selectedColumn.getAlias())) {
                    outputName = selectedColumn.getAlias().trim();
                    fragment += " AS " + quoteIdentifier(outputName);
                }
                fragments.add(fragment);
                projectedColumns.add(outputName);
            }
        }
        sql.append(String.join(", ", fragments));
        return projectedColumns;
    }

    private List<String> appendAggregationSelect(StringBuilder sql,
                                                 AggregationRequestDto aggregations,
                                                 Map<String, SchemaColumn> schemaByName) {
        if (CollectionUtils.isEmpty(aggregations.getFunctions())) {
            throw new InvalidQueryException("At least one aggregation function is required.");
        }

        List<String> fragments = new ArrayList<>();
        List<String> projectedColumns = new ArrayList<>();
        if (!CollectionUtils.isEmpty(aggregations.getGroupedBy())) {
            for (String groupedBy : aggregations.getGroupedBy()) {
                SchemaColumn column = resolveColumn(groupedBy, schemaByName);
                fragments.add(quoteIdentifier(column.name()));
                projectedColumns.add(column.name());
            }
        }

        for (AggregationFunctionDto functionDto : aggregations.getFunctions()) {
            SchemaColumn column = resolveColumn(functionDto.getColumn(), schemaByName);
            String function = normalizeRequired(functionDto.getFunction(), "Aggregation function is required.").toUpperCase(Locale.ROOT);
            validateAggregationFunction(function, column);
            String alias = StringUtils.hasText(functionDto.getAlias())
                    ? functionDto.getAlias().trim()
                    : function.toLowerCase(Locale.ROOT) + "_" + column.name();
            fragments.add(function + "(" + quoteIdentifier(column.name()) + ") AS " + quoteIdentifier(alias));
            projectedColumns.add(alias);
        }
        sql.append(String.join(", ", fragments));
        return projectedColumns;
    }

    private String compileWhereClause(List<FilterGroupDto> groups,
                                      Map<String, SchemaColumn> schemaByName,
                                      List<Object> parameters) {
        if (CollectionUtils.isEmpty(groups)) {
            return null;
        }
        List<String> compiledGroups = new ArrayList<>();
        for (FilterGroupDto group : groups) {
            compiledGroups.add(compileGroup(group, schemaByName, parameters));
        }
        return String.join(" AND ", compiledGroups);
    }

    private String compileGroup(FilterGroupDto group,
                                Map<String, SchemaColumn> schemaByName,
                                List<Object> parameters) {
        String operator = normalizeRequired(group.getOperator(), "Filter group operator is required.").toUpperCase(Locale.ROOT);
        if (!"AND".equals(operator) && !"OR".equals(operator)) {
            throw new InvalidQueryException("Unsupported filter group operator: " + operator);
        }

        List<String> parts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(group.getConditions())) {
            for (FilterConditionDto condition : group.getConditions()) {
                parts.add(compileCondition(condition, schemaByName, parameters));
            }
        }
        if (!CollectionUtils.isEmpty(group.getGroups())) {
            for (FilterGroupDto nestedGroup : group.getGroups()) {
                parts.add(compileGroup(nestedGroup, schemaByName, parameters));
            }
        }
        if (parts.isEmpty()) {
            throw new InvalidQueryException("A filter group must contain at least one condition or subgroup.");
        }
        return "(" + String.join(" " + operator + " ", parts) + ")";
    }

    private String compileCondition(FilterConditionDto condition,
                                    Map<String, SchemaColumn> schemaByName,
                                    List<Object> parameters) {
        SchemaColumn column = resolveColumn(condition.getColumn(), schemaByName);
        String operator = normalizeRequired(condition.getOperator(), "Filter operator is required.").toUpperCase(Locale.ROOT);
        boolean ignoreCase = Boolean.TRUE.equals(condition.getIgnoreCase());
        String expression = quoteIdentifier(column.name());
        String valueExpression = ignoreCase ? "LOWER(" + expression + ")" : expression;

        return switch (operator) {
            case "EQ" -> binaryComparison(condition.getValue(), parameters, valueExpression, ignoreCase, "=");
            case "NEQ" -> binaryComparison(condition.getValue(), parameters, valueExpression, ignoreCase, "<>");
            case "GT" -> simpleComparison(condition.getValue(), parameters, expression, ">");
            case "GTE" -> simpleComparison(condition.getValue(), parameters, expression, ">=");
            case "LT" -> simpleComparison(condition.getValue(), parameters, expression, "<");
            case "LTE" -> simpleComparison(condition.getValue(), parameters, expression, "<=");
            case "IN" -> inComparison(condition.getValues(), parameters, valueExpression, ignoreCase, false);
            case "NOT_IN" -> inComparison(condition.getValues(), parameters, valueExpression, ignoreCase, true);
            case "CONTAINS" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "LIKE", "%", "%");
            case "NOT_CONTAINS" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "NOT LIKE", "%", "%");
            case "STARTS_WITH" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "LIKE", "", "%");
            case "NOT_STARTS_WITH" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "NOT LIKE", "", "%");
            case "ENDS_WITH" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "LIKE", "%", "");
            case "NOT_ENDS_WITH" -> stringComparison(condition.getValue(), parameters, column, valueExpression, ignoreCase, "NOT LIKE", "%", "");
            case "IS_NULL" -> expression + " IS NULL";
            case "IS_NOT_NULL" -> expression + " IS NOT NULL";
            case "IS_TRUE" -> booleanComparison(column, expression, true);
            case "IS_FALSE" -> booleanComparison(column, expression, false);
            default -> throw new InvalidQueryException("Unsupported filter operator: " + operator);
        };
    }

    private String binaryComparison(Object value,
                                    List<Object> parameters,
                                    String expression,
                                    boolean ignoreCase,
                                    String sqlOperator) {
        parameters.add(normalizeScalarValue(value, ignoreCase));
        return expression + " " + sqlOperator + " ?";
    }

    private String simpleComparison(Object value, List<Object> parameters, String expression, String sqlOperator) {
        if (value == null) {
            throw new InvalidQueryException("A comparison value is required.");
        }
        parameters.add(value);
        return expression + " " + sqlOperator + " ?";
    }

    private String inComparison(List<Object> values,
                                List<Object> parameters,
                                String expression,
                                boolean ignoreCase,
                                boolean negated) {
        if (CollectionUtils.isEmpty(values)) {
            throw new InvalidQueryException("A non-empty values array is required.");
        }
        List<String> placeholders = new ArrayList<>();
        for (Object value : values) {
            parameters.add(normalizeScalarValue(value, ignoreCase));
            placeholders.add("?");
        }
        return expression + (negated ? " NOT IN (" : " IN (") + String.join(", ", placeholders) + ")";
    }

    private String stringComparison(Object value,
                                    List<Object> parameters,
                                    SchemaColumn column,
                                    String expression,
                                    boolean ignoreCase,
                                    String sqlOperator,
                                    String prefix,
                                    String suffix) {
        validateStringColumn(column);
        if (value == null) {
            throw new InvalidQueryException("A string comparison value is required.");
        }
        parameters.add(prefix + normalizeScalarValue(value, ignoreCase) + suffix);
        return expression + " " + sqlOperator + " ?";
    }

    private String booleanComparison(SchemaColumn column, String expression, boolean value) {
        if (!isBooleanType(column.typeName())) {
            throw new InvalidQueryException("Operator requires a boolean column: " + column.name());
        }
        return expression + (value ? " IS TRUE" : " IS FALSE");
    }

    private void validateAggregationFunction(String function, SchemaColumn column) {
        switch (function) {
            case "COUNT", "MIN", "MAX" -> {
                return;
            }
            case "SUM", "AVG" -> {
                if (!isNumericType(column.typeName())) {
                    throw new InvalidQueryException(function + " requires a numeric column: " + column.name());
                }
                return;
            }
            default -> throw new InvalidQueryException("Unsupported aggregation function: " + function);
        }
    }

    private SchemaColumn resolveColumn(String requestedColumn, Map<String, SchemaColumn> schemaByName) {
        String normalized = normalizeRequired(requestedColumn, "Column name is required.").toLowerCase(Locale.ROOT);
        SchemaColumn column = schemaByName.get(normalized);
        if (column == null) {
            throw new InvalidQueryException("Unknown column: " + requestedColumn);
        }
        return column;
    }

    private Object normalizeScalarValue(Object value, boolean ignoreCase) {
        if (value == null) {
            throw new InvalidQueryException("A value is required.");
        }
        if (ignoreCase && value instanceof String stringValue) {
            return stringValue.toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private void validateStringColumn(SchemaColumn column) {
        if (!isStringType(column.typeName())) {
            throw new InvalidQueryException("Operator requires a string column: " + column.name());
        }
    }

    private boolean isStringType(String typeName) {
        String normalized = typeName.toUpperCase(Locale.ROOT);
        return normalized.contains("CHAR") || normalized.contains("TEXT") || normalized.contains("STRING") || normalized.contains("VARCHAR");
    }

    private boolean isNumericType(String typeName) {
        String normalized = typeName.toUpperCase(Locale.ROOT);
        return normalized.contains("INT") || normalized.contains("DECIMAL") || normalized.contains("NUMERIC")
                || normalized.contains("DOUBLE") || normalized.contains("FLOAT") || normalized.contains("REAL")
                || normalized.contains("HUGEINT") || normalized.contains("BIGINT") || normalized.contains("SMALLINT");
    }

    private boolean isBooleanType(String typeName) {
        return typeName.toUpperCase(Locale.ROOT).contains("BOOL");
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidQueryException(message);
        }
        return value.trim();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public record SchemaColumn(String name, String typeName) {
    }

    public record QueryPlan(String sql, List<Object> parameters, List<String> projectedColumns) {
    }
}