package fr.grozeille.db4all.api.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.ConnectionTestResponse;
import fr.grozeille.db4all.api.dto.DatasourceCreationRequest;
import fr.grozeille.db4all.api.dto.DatasourceDto;
import fr.grozeille.db4all.api.dto.DatasourceScanRequest;
import fr.grozeille.db4all.api.dto.DatasourceScanResponse;
import fr.grozeille.db4all.api.dto.DatasourceUpdateRequest;
import fr.grozeille.db4all.api.dto.LocalFilesystemDatasourceConfigurationDto;
import fr.grozeille.db4all.api.dto.ScanItemDto;
import fr.grozeille.db4all.api.exceptions.DatasourceNotFoundException;
import fr.grozeille.db4all.api.model.Datasource;
import fr.grozeille.db4all.api.model.DatasourceType;
import fr.grozeille.db4all.api.repository.DatasourceRepository;
import fr.grozeille.db4all.api.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceRepository datasourceRepository;
    private final TableRepository tableRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DatasourceDto> findAll(String projectId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        return datasourceRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DatasourceDto findById(String projectId, String datasourceId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        return toDto(getEntityById(projectId, datasourceId));
    }

    @Transactional
    public DatasourceDto create(String projectId, DatasourceCreationRequest request, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        validateName(projectId, request.getName(), null);
        LocalFilesystemDatasourceConfigurationDto configuration = validateAndNormalize(request.getType(), request.getConfiguration());

        Datasource datasource = Datasource.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .type(request.getType())
                .readOnly(request.isReadOnly())
                .configurationJson(writeConfiguration(configuration))
                .build();

        return toDto(datasourceRepository.save(datasource));
    }

    @Transactional
    public DatasourceDto update(String projectId,
                                String datasourceId,
                                DatasourceUpdateRequest request,
                                Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Datasource datasource = getEntityById(projectId, datasourceId);
        validateName(projectId, request.getName(), datasourceId);
        LocalFilesystemDatasourceConfigurationDto configuration = validateAndNormalize(request.getType(), request.getConfiguration());

        datasource.setName(request.getName().trim());
        datasource.setDescription(normalizeDescription(request.getDescription()));
        datasource.setType(request.getType());
        datasource.setReadOnly(request.isReadOnly());
        datasource.setConfigurationJson(writeConfiguration(configuration));

        return toDto(datasourceRepository.save(datasource));
    }

    @Transactional
    public void delete(String projectId, String datasourceId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Datasource datasource = getEntityById(projectId, datasourceId);
        if (tableRepository.existsByDatasourceId(datasource.getId())) {
            throw new IllegalArgumentException("Datasource is still used by at least one table.");
        }
        datasourceRepository.delete(datasource);
    }

    @Transactional(readOnly = true)
    public ConnectionTestResponse testConnection(String projectId, String datasourceId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Datasource datasource = getEntityById(projectId, datasourceId);
        LocalFilesystemDatasourceConfigurationDto configuration = readConfiguration(datasource.getConfigurationJson());
        Path rootPath = normalizeExistingDirectory(configuration.getRootPath());
        return ConnectionTestResponse.builder()
                .success(true)
                .message("Directory is reachable: " + rootPath)
                .build();
    }

    @Transactional(readOnly = true)
    public DatasourceScanResponse scan(String projectId,
                                       String datasourceId,
                                       DatasourceScanRequest request,
                                       Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        Datasource datasource = getEntityById(projectId, datasourceId);
        if (datasource.getType() != DatasourceType.LOCAL_FILESYSTEM) {
            throw new IllegalArgumentException("Only LOCAL_FILESYSTEM datasources are supported in the current MVP.");
        }
        if (request == null || request.getScanType() == null) {
            throw new IllegalArgumentException("Scan type is required.");
        }
        Path file = resolveFilePath(datasource, request.getFilePath());
        return switch (request.getScanType()) {
            case EXCEL_SHEETS -> DatasourceScanResponse.builder()
                    .items(TableExcelSource.listSheets(file).stream()
                            .map(sheet -> ScanItemDto.builder()
                                    .id(sheet.name())
                                    .label(sheet.name())
                                    .itemType("SHEET")
                                    .metadata(Map.of(
                                            "index", sheet.index(),
                                            "hidden", sheet.hidden(),
                                            "rows", sheet.rows()))
                                    .build())
                            .toList())
                    .build();
            case EXCEL_NAMED_RANGES -> DatasourceScanResponse.builder()
                    .items(TableExcelSource.listNamedRanges(file).stream()
                            .map(range -> {
                                Map<String, Object> metadata = new LinkedHashMap<>();
                                metadata.put("formula", range.formula());
                                if (range.sheetName() != null) {
                                    metadata.put("sheetName", range.sheetName());
                                }
                                return ScanItemDto.builder()
                                        .id(range.name())
                                        .label(range.name())
                                        .itemType("NAMED_RANGE")
                                        .metadata(metadata)
                                        .build();
                            })
                            .toList())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Scan type " + request.getScanType() + " is not supported in the current MVP.");
        };
    }

    @Transactional(readOnly = true)
    public Path resolveFilePath(Datasource datasource, String relativePath) {
        LocalFilesystemDatasourceConfigurationDto configuration = getLocalFilesystemConfiguration(datasource);
        Path root = Paths.get(configuration.getRootPath()).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("Datasource rootPath must point to an existing directory.");
        }
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("File path is required.");
        }
        String trimmedPath = relativePath.trim();
        if (trimmedPath.contains("*") || trimmedPath.contains("?") || trimmedPath.contains("[")) {
            throw new IllegalArgumentException("File path must point to a single file, wildcards are not supported.");
        }
        Path candidate = root.resolve(trimmedPath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("File path must stay within the datasource root.");
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IllegalArgumentException("File does not exist: " + trimmedPath);
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public Datasource getEntityById(String projectId, String datasourceId) {
        return datasourceRepository.findByIdAndProjectId(datasourceId, projectId)
                .orElseThrow(() -> new DatasourceNotFoundException(datasourceId));
    }

    @Transactional(readOnly = true)
    public LocalFilesystemDatasourceConfigurationDto getLocalFilesystemConfiguration(Datasource datasource) {
        if (datasource.getType() != DatasourceType.LOCAL_FILESYSTEM) {
            throw new IllegalArgumentException("Only LOCAL_FILESYSTEM datasources are supported in the current MVP.");
        }
        return readConfiguration(datasource.getConfigurationJson());
    }

    private void validateName(String projectId, String name, String datasourceId) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Datasource name is required.");
        }
        boolean exists = datasourceId == null
                ? datasourceRepository.existsByProjectIdAndNameIgnoreCase(projectId, name.trim())
                : datasourceRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name.trim(), datasourceId);
        if (exists) {
            throw new IllegalArgumentException("Datasource name is already used in this project.");
        }
    }

    private LocalFilesystemDatasourceConfigurationDto validateAndNormalize(DatasourceType type,
                                                                           LocalFilesystemDatasourceConfigurationDto configuration) {
        if (type != DatasourceType.LOCAL_FILESYSTEM) {
            throw new IllegalArgumentException("Only LOCAL_FILESYSTEM datasources are supported in the current MVP.");
        }
        if (configuration == null || !StringUtils.hasText(configuration.getRootPath())) {
            throw new IllegalArgumentException("Datasource rootPath is required.");
        }
        Path normalizedPath = normalizeExistingDirectory(configuration.getRootPath());
        return LocalFilesystemDatasourceConfigurationDto.builder()
                .rootPath(normalizedPath.toString())
                .build();
    }

    private Path normalizeExistingDirectory(String rootPath) {
        Path normalizedPath = Paths.get(rootPath).toAbsolutePath().normalize();
        if (!Files.exists(normalizedPath) || !Files.isDirectory(normalizedPath)) {
            throw new IllegalArgumentException("Datasource rootPath must point to an existing directory.");
        }
        if (!Files.isReadable(normalizedPath)) {
            throw new IllegalArgumentException("Datasource rootPath must be readable.");
        }
        return normalizedPath;
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }

    private String writeConfiguration(LocalFilesystemDatasourceConfigurationDto configuration) {
        try {
            return objectMapper.writeValueAsString(configuration);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to serialize datasource configuration.", exception);
        }
    }

    private LocalFilesystemDatasourceConfigurationDto readConfiguration(String configurationJson) {
        try {
            return objectMapper.readValue(configurationJson, LocalFilesystemDatasourceConfigurationDto.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to deserialize datasource configuration.", exception);
        }
    }

    private DatasourceDto toDto(Datasource datasource) {
        return DatasourceDto.builder()
                .id(datasource.getId())
                .name(datasource.getName())
                .description(normalizeDescription(datasource.getDescription()))
                .type(datasource.getType())
                .readOnly(datasource.isReadOnly())
                .configuration(readConfiguration(datasource.getConfigurationJson()))
                .build();
    }
}