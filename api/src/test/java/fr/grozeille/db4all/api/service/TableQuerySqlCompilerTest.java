package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.dto.AggregationFunctionDto;
import fr.grozeille.db4all.api.dto.AggregationRequestDto;
import fr.grozeille.db4all.api.dto.FilterConditionDto;
import fr.grozeille.db4all.api.dto.FilterGroupDto;
import fr.grozeille.db4all.api.dto.SelectedColumnDto;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.exceptions.InvalidQueryException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableQuerySqlCompilerTest {

    private final TableQuerySqlCompiler compiler = new TableQuerySqlCompiler();

    @Test
    void shouldCompileDistinctProjectionWithAliasAndPagination() {
        TableQueryRequest request = TableQueryRequest.builder()
                .selectedColumns(List.of(SelectedColumnDto.builder()
                        .column("customerName")
                        .alias("customer_name")
                        .build()))
                .distinct(true)
                .page(1)
                .size(25)
                .build();

        TableQuerySqlCompiler.QueryPlan plan = compiler.compile(
                "SELECT * FROM source_table",
                List.of(),
                schema(),
                request,
                true
        );

        assertTrue(plan.sql().contains("SELECT DISTINCT \"customerName\" AS \"customer_name\""));
        assertTrue(plan.sql().contains("LIMIT ? OFFSET ?"));
        assertEquals(List.of(25, 25), plan.parameters());
    }

    @Test
    void shouldCompileCaseInsensitiveContainsAndInFilters() {
        TableQueryRequest request = TableQueryRequest.builder()
                .filters(List.of(FilterGroupDto.builder()
                        .operator("AND")
                        .conditions(List.of(
                                FilterConditionDto.builder()
                                        .column("customerName")
                                        .operator("CONTAINS")
                                        .value("smith")
                                        .ignoreCase(true)
                                        .build(),
                                FilterConditionDto.builder()
                                        .column("status")
                                        .operator("IN")
                                        .values(List.of("ACTIVE", "INACTIVE"))
                                        .build()
                        ))
                        .build()))
                .build();

        TableQuerySqlCompiler.QueryPlan plan = compiler.compile(
                "SELECT * FROM source_table",
                List.of(),
                schema(),
                request,
                true
        );

        assertTrue(plan.sql().contains("LOWER(\"customerName\") LIKE ?"));
        assertTrue(plan.sql().contains("\"status\" IN (?, ?)"));
        assertEquals(List.of("%smith%", "ACTIVE", "INACTIVE", 100, 0), plan.parameters());
    }

    @Test
    void shouldCompileAggregationQuery() {
        TableQueryRequest request = TableQueryRequest.builder()
                .aggregations(AggregationRequestDto.builder()
                        .groupedBy(List.of("country"))
                        .functions(List.of(AggregationFunctionDto.builder()
                                .column("amount")
                                .function("SUM")
                                .alias("total_amount")
                                .build()))
                        .build())
                .build();

        TableQuerySqlCompiler.QueryPlan plan = compiler.compile(
                "SELECT * FROM source_table",
                List.of(),
                schema(),
                request,
                false
        );

        assertTrue(plan.sql().contains("SELECT \"country\", SUM(\"amount\") AS \"total_amount\""));
        assertTrue(plan.sql().contains("GROUP BY \"country\""));
    }

    @Test
    void shouldRejectNumericAggregationOnStringColumn() {
        TableQueryRequest request = TableQueryRequest.builder()
                .aggregations(AggregationRequestDto.builder()
                        .functions(List.of(AggregationFunctionDto.builder()
                                .column("customerName")
                                .function("SUM")
                                .build()))
                        .build())
                .build();

        assertThrows(InvalidQueryException.class, () -> compiler.compile(
                "SELECT * FROM source_table",
                List.of(),
                schema(),
                request,
                false
        ));
    }

    @Test
    void shouldRejectUnknownColumnInjectionAttempt() {
        TableQueryRequest request = TableQueryRequest.builder()
                .selectedColumns(List.of(SelectedColumnDto.builder()
                        .column("customerName\"; DROP TABLE views; --")
                        .build()))
                .build();

        assertThrows(InvalidQueryException.class, () -> compiler.compile(
                "SELECT * FROM source_table",
                List.of(),
                schema(),
                request,
                false
        ));
    }

    private List<TableQuerySqlCompiler.SchemaColumn> schema() {
        return List.of(
                new TableQuerySqlCompiler.SchemaColumn("customerName", "VARCHAR"),
                new TableQuerySqlCompiler.SchemaColumn("status", "VARCHAR"),
                new TableQuerySqlCompiler.SchemaColumn("country", "VARCHAR"),
                new TableQuerySqlCompiler.SchemaColumn("amount", "DECIMAL")
        );
    }
}