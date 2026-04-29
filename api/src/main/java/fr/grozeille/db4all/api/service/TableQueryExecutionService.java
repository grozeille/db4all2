package fr.grozeille.db4all.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.ColumnDefinitionDto;
import fr.grozeille.db4all.api.dto.LocalFilesystemDatasourceConfigurationDto;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.dto.TableQueryResponse;
import fr.grozeille.db4all.api.model.Datasource;
import fr.grozeille.db4all.api.model.DatasourceType;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.repository.DatasourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableQueryExecutionService {

    private static final String DUCKDB_JDBC_URL = "jdbc:duckdb:";

    private final TableService tableService;
    private final DatasourceRepository datasourceRepository;
    private final DatasourceService datasourceService;
    private final ProjectAccessService projectAccessService;
    private final TableQuerySqlCompiler tableQuerySqlCompiler;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TableQueryResponse queryTable(String projectId,
                                        String tableId,
                                        TableQueryRequest request,
                                        Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Table table = tableService.getEntityById(tableId, projectId);
        Datasource datasource = getDatasource(projectId, table.getDatasourceId());
        return execute(table, datasource, request, true);
    }

    @Transactional(readOnly = true)
    public String compileTableQuery(String projectId,
                                    String tableId,
                                    TableQueryRequest request,
                                    Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Table table = tableService.getEntityById(tableId, projectId);
        Datasource datasource = getDatasource(projectId, table.getDatasourceId());
        PreparedSource preparedSource = prepareSource(table, datasource);
        List<TableQuerySqlCompiler.SchemaColumn> schema = inspectSchema(preparedSource);
        return tableQuerySqlCompiler.compile(preparedSource.sourceSql(), preparedSource.parameters(), schema, request, false).sql();
    }

    @Transactional(readOnly = true)
    public TableQueryResponse queryView(Table sourceTable,
                                        String projectId,
                                        TableQueryRequest request,
                                        Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Datasource datasource = getDatasource(projectId, sourceTable.getDatasourceId());
        return execute(sourceTable, datasource, request, true);
    }

    private TableQueryResponse execute(Table table,
                                       Datasource datasource,
                                       TableQueryRequest request,
                                       boolean applyPagination) {
        PreparedSource preparedSource = prepareSource(table, datasource);
        List<TableQuerySqlCompiler.SchemaColumn> schema = inspectSchema(preparedSource);
        TableQuerySqlCompiler.QueryPlan plan = tableQuerySqlCompiler.compile(
                preparedSource.sourceSql(),
                preparedSource.parameters(),
                schema,
                request,
                applyPagination
        );

        try (Connection connection = DriverManager.getConnection(DUCKDB_JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(plan.sql())) {
            bindParameters(statement, plan.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<ColumnDefinitionDto> columns = extractColumns(metaData);
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= metaData.getColumnCount(); index++) {
                        row.put(metaData.getColumnLabel(index), resultSet.getObject(index));
                    }
                    rows.add(row);
                }
                return TableQueryResponse.builder()
                        .columns(columns)
                        .rows(rows)
                        .build();
            }
        } catch (SQLException exception) {
            log.error("DuckDB query execution failed for table {} in project {}", table.getId(), table.getProjectId(), exception);
            throw new IllegalStateException("Unable to execute the query.", exception);
        }
    }

    private Datasource getDatasource(String projectId, String datasourceId) {
        return datasourceRepository.findByIdAndProjectId(datasourceId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + datasourceId));
    }

    private PreparedSource prepareSource(Table table, Datasource datasource) {
        if (datasource.getType() != DatasourceType.LOCAL_FILESYSTEM) {
            throw new IllegalArgumentException("Only LOCAL_FILESYSTEM datasources are supported in the current MVP.");
        }
        JsonNode configuration = readTableConfiguration(table.getConfigurationJson());
        String relativePath = configuration.path("path").asText(null);
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("Table configuration path is required.");
        }
        LocalFilesystemDatasourceConfigurationDto datasourceConfiguration = datasourceService.getLocalFilesystemConfiguration(datasource);
        String resolvedPath = resolveSourcePath(datasourceConfiguration.getRootPath(), relativePath);
        String sourceSql = switch (table.getSourceKind()) {
            case CSV -> "SELECT * FROM read_csv_auto(?)";
            case PARQUET -> "SELECT * FROM read_parquet(?)";
        };
        return new PreparedSource(sourceSql, List.of(resolvedPath));
    }

    private String resolveSourcePath(String rootPath, String relativePath) {
        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Datasource rootPath must point to an existing directory.");
        }
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Table path must stay within the datasource root.");
        }
        boolean hasGlob = relativePath.contains("*") || relativePath.contains("?") || relativePath.contains("[");
        if (!hasGlob && !Files.exists(candidate)) {
            throw new IllegalArgumentException("Table path does not exist: " + relativePath);
        }
        return candidate.toString();
    }

    private JsonNode readTableConfiguration(String configurationJson) {
        try {
            return objectMapper.readTree(configurationJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize table configuration.", exception);
        }
    }

    private List<TableQuerySqlCompiler.SchemaColumn> inspectSchema(PreparedSource preparedSource) {
        String schemaSql = "SELECT * FROM (" + preparedSource.sourceSql() + ") source_data LIMIT 0";
        try (Connection connection = DriverManager.getConnection(DUCKDB_JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(schemaSql)) {
            bindParameters(statement, preparedSource.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<TableQuerySqlCompiler.SchemaColumn> schema = new ArrayList<>();
                for (int index = 1; index <= metaData.getColumnCount(); index++) {
                    schema.add(new TableQuerySqlCompiler.SchemaColumn(
                            metaData.getColumnLabel(index),
                            metaData.getColumnTypeName(index)
                    ));
                }
                return schema;
            }
        } catch (SQLException exception) {
            log.error("DuckDB schema inspection failed", exception);
            throw new IllegalStateException("Unable to inspect the table schema.", exception);
        }
    }

    private List<ColumnDefinitionDto> extractColumns(ResultSetMetaData metaData) throws SQLException {
        List<ColumnDefinitionDto> columns = new ArrayList<>();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            columns.add(ColumnDefinitionDto.builder()
                    .name(metaData.getColumnLabel(index))
                    .dataType(metaData.getColumnTypeName(index))
                    .build());
        }
        return columns;
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private record PreparedSource(String sourceSql, List<Object> parameters) {
    }
}