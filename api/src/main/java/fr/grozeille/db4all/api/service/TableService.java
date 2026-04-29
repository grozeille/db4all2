package fr.grozeille.db4all.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.TableCreationRequest;
import fr.grozeille.db4all.api.dto.TableDto;
import fr.grozeille.db4all.api.dto.TableUpdateRequest;
import fr.grozeille.db4all.api.exceptions.DatasourceNotFoundException;
import fr.grozeille.db4all.api.exceptions.TableNotFoundException;
import fr.grozeille.db4all.api.model.Datasource;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.repository.DatasourceRepository;
import fr.grozeille.db4all.api.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final DatasourceRepository datasourceRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<TableDto> findAll(String projectId, String search, Pageable pageable, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        if (search != null && !search.isEmpty()) {
            return tableRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, search, pageable)
                    .map(this::toDto);
        } else {
            return tableRepository.findByProjectId(projectId, pageable)
                    .map(this::toDto);
        }
    }

    @Transactional(readOnly = true)
    public TableDto findById(String tableId, String projectId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        return toDto(getEntityById(tableId, projectId));
    }

    @Transactional
    public TableDto create(String projectId, TableCreationRequest request, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        validateTableNameUniqueness(projectId, request.getName(), null);
        Datasource datasource = getDatasource(projectId, request.getDatasourceId());
        validateConfiguration(request.getSourceKind().name(), request.getConfiguration());

        Table table = Table.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .datasourceId(datasource.getId())
                .sourceKind(request.getSourceKind())
                .configurationJson(writeJson(request.getConfiguration()))
                .build();

        return toDto(tableRepository.save(table));
    }

    @Transactional
    public TableDto update(String tableId, String projectId, TableUpdateRequest request, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Table table = getEntityById(tableId, projectId);
        validateTableNameUniqueness(projectId, request.getName(), tableId);
        Datasource datasource = getDatasource(projectId, request.getDatasourceId());
        validateConfiguration(request.getSourceKind().name(), request.getConfiguration());

        table.setName(request.getName().trim());
        table.setDescription(normalizeDescription(request.getDescription()));
        table.setDatasourceId(datasource.getId());
        table.setSourceKind(request.getSourceKind());
        table.setConfigurationJson(writeJson(request.getConfiguration()));

        return toDto(tableRepository.save(table));
    }

    @Transactional
    public void delete(String tableId, String projectId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        if (!tableRepository.existsByIdAndProjectId(tableId, projectId)) {
            throw new TableNotFoundException(tableId);
        }
        tableRepository.deleteById(tableId);
    }

    @Transactional(readOnly = true)
    public Table getEntityById(String tableId, String projectId) {
        return tableRepository.findByIdAndProjectId(tableId, projectId)
                .orElseThrow(() -> new TableNotFoundException(tableId));
    }

    private Datasource getDatasource(String projectId, String datasourceId) {
        return datasourceRepository.findByIdAndProjectId(datasourceId, projectId)
                .orElseThrow(() -> new DatasourceNotFoundException(datasourceId));
    }

    private void validateTableNameUniqueness(String projectId, String name, String tableId) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Table name is required.");
        }
        boolean exists = tableId == null
                ? tableRepository.existsByProjectIdAndNameIgnoreCase(projectId, name.trim())
                : tableRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name.trim(), tableId);
        if (exists) {
            throw new IllegalArgumentException("Table name is already used in this project.");
        }
    }

    private void validateConfiguration(String sourceKind, JsonNode configuration) {
        if (configuration == null || !configuration.isObject()) {
            throw new IllegalArgumentException("Table configuration is required.");
        }
        String path = configuration.path("path").asText(null);
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Table configuration path is required.");
        }
        if (!"CSV".equals(sourceKind) && !"PARQUET".equals(sourceKind)) {
            throw new IllegalArgumentException("Only CSV and PARQUET tables are supported in the current MVP.");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }

    private String writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize table configuration.", exception);
        }
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize table configuration.", exception);
        }
    }

    private TableDto toDto(Table table) {
        return TableDto.builder()
                .id(table.getId())
                .name(table.getName())
                .description(normalizeDescription(table.getDescription()))
                .datasourceId(table.getDatasourceId())
                .sourceKind(table.getSourceKind())
                .configuration(readJson(table.getConfigurationJson()))
                .build();
    }
}
