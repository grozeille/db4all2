package fr.grozeille.db4all.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.dto.TableQueryResponse;
import fr.grozeille.db4all.api.dto.ViewCreationRequest;
import fr.grozeille.db4all.api.dto.ViewDto;
import fr.grozeille.db4all.api.dto.ViewUpdateRequest;
import fr.grozeille.db4all.api.exceptions.ViewNotFoundException;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.model.View;
import fr.grozeille.db4all.api.model.ViewType;
import fr.grozeille.db4all.api.repository.ViewRepository;
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
public class ViewService {

    private final ViewRepository viewRepository;
    private final ProjectAccessService projectAccessService;
    private final TableService tableService;
    private final TableQueryExecutionService tableQueryExecutionService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ViewDto> findAll(String projectId, String search, Pageable pageable, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        if (StringUtils.hasText(search)) {
            return viewRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, search, pageable)
                    .map(this::toDto);
        }
        return viewRepository.findByProjectId(projectId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ViewDto findById(String projectId, String viewId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        return toDto(getEntityById(projectId, viewId));
    }

    @Transactional
    public ViewDto create(String projectId, ViewCreationRequest request, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        validateName(projectId, request.getName(), null);
        validateType(request.getType());
        Table sourceTable = tableService.getEntityById(request.getSourceTableId(), projectId);
        TableQueryRequest storedQuery = normalizeStoredQuery(request.getQuery());
        String compiledSql = tableQueryExecutionService.compileTableQuery(projectId, sourceTable.getId(), storedQuery, authentication);

        View view = View.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .type(request.getType())
                .sourceTableId(sourceTable.getId())
                .queryJson(writeQuery(storedQuery))
                .compiledSql(compiledSql)
                .build();

        return toDto(viewRepository.save(view));
    }

    @Transactional
    public ViewDto update(String projectId, String viewId, ViewUpdateRequest request, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        View view = getEntityById(projectId, viewId);
        validateName(projectId, request.getName(), viewId);
        validateType(request.getType());
        Table sourceTable = tableService.getEntityById(request.getSourceTableId(), projectId);
        TableQueryRequest storedQuery = normalizeStoredQuery(request.getQuery());
        String compiledSql = tableQueryExecutionService.compileTableQuery(projectId, sourceTable.getId(), storedQuery, authentication);

        view.setName(request.getName().trim());
        view.setDescription(normalizeDescription(request.getDescription()));
        view.setType(request.getType());
        view.setSourceTableId(sourceTable.getId());
        view.setQueryJson(writeQuery(storedQuery));
        view.setCompiledSql(compiledSql);

        return toDto(viewRepository.save(view));
    }

    @Transactional
    public void delete(String projectId, String viewId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        View view = getEntityById(projectId, viewId);
        viewRepository.delete(view);
    }

    @Transactional(readOnly = true)
    public TableQueryResponse query(String projectId,
                                    String viewId,
                                    TableQueryRequest runtimeRequest,
                                    Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        View view = getEntityById(projectId, viewId);
        Table sourceTable = tableService.getEntityById(view.getSourceTableId(), projectId);
        TableQueryRequest mergedQuery = mergeQuery(readQuery(view.getQueryJson()), runtimeRequest);
        return tableQueryExecutionService.queryView(sourceTable, projectId, mergedQuery, authentication);
    }

    private View getEntityById(String projectId, String viewId) {
        return viewRepository.findByIdAndProjectId(viewId, projectId)
                .orElseThrow(() -> new ViewNotFoundException(viewId));
    }

    private void validateName(String projectId, String name, String viewId) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("View name is required.");
        }
        boolean exists = viewId == null
                ? viewRepository.existsByProjectIdAndNameIgnoreCase(projectId, name.trim())
                : viewRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name.trim(), viewId);
        if (exists) {
            throw new IllegalArgumentException("View name is already used in this project.");
        }
    }

    private void validateType(ViewType type) {
        if (type != ViewType.FILTER) {
            throw new IllegalArgumentException("Only FILTER views are supported in the current MVP.");
        }
    }

    private TableQueryRequest normalizeStoredQuery(TableQueryRequest request) {
        return TableQueryRequest.builder()
                .selectedColumns(request.getSelectedColumns())
                .distinct(request.getDistinct())
                .filters(request.getFilters())
                .aggregations(request.getAggregations())
                .page(null)
                .size(null)
                .build();
    }

    private TableQueryRequest mergeQuery(TableQueryRequest storedQuery, TableQueryRequest runtimeRequest) {
        if (runtimeRequest != null && (runtimeRequest.getSelectedColumns() != null
                || runtimeRequest.getDistinct() != null
                || runtimeRequest.getFilters() != null
                || runtimeRequest.getAggregations() != null)) {
            throw new IllegalArgumentException("Only page and size overrides are supported when querying a saved view.");
        }

        Integer page = runtimeRequest == null ? null : runtimeRequest.getPage();
        Integer size = runtimeRequest == null ? null : runtimeRequest.getSize();
        return TableQueryRequest.builder()
                .selectedColumns(storedQuery.getSelectedColumns())
                .distinct(storedQuery.getDistinct())
                .filters(storedQuery.getFilters())
                .aggregations(storedQuery.getAggregations())
                .page(page)
                .size(size)
                .build();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }

    private String writeQuery(TableQueryRequest query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize the view query.", exception);
        }
    }

    private TableQueryRequest readQuery(String queryJson) {
        try {
            return objectMapper.readValue(queryJson, TableQueryRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize the view query.", exception);
        }
    }

    private ViewDto toDto(View view) {
        return ViewDto.builder()
                .id(view.getId())
                .name(view.getName())
                .description(normalizeDescription(view.getDescription()))
                .type(view.getType())
                .sourceTableId(view.getSourceTableId())
                .query(readQuery(view.getQueryJson()))
                .build();
    }
}