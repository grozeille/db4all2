package fr.grozeille.db4all.api.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.SortDto;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.dto.TableQueryResponse;
import fr.grozeille.db4all.api.dto.ViewCreationRequest;
import fr.grozeille.db4all.api.dto.ViewDto;
import fr.grozeille.db4all.api.dto.ViewPreviewRequest;
import fr.grozeille.db4all.api.dto.ViewTransformationStepDto;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViewService {

    private static final Set<String> STEP_TYPES = Set.of("FILTER", "RENAME", "DROP", "REORDER", "SORT", "COLUMNS");

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
        List<ViewTransformationStepDto> steps = normalizeSteps(request.getSteps());
        TableQueryRequest storedQuery = normalizeStoredQuery(request.getQuery());

        Table rootTable;
        String parentViewId = null;
        List<TableQueryRequest> chainQueries = new ArrayList<>();
        if (StringUtils.hasText(request.getParentViewId())) {
            View parent = getEntityById(projectId, request.getParentViewId().trim());
            rootTable = tableService.getEntityById(parent.getSourceTableId(), projectId);
            if (StringUtils.hasText(request.getSourceTableId())
                    && !rootTable.getId().equals(request.getSourceTableId().trim())) {
                throw new IllegalArgumentException("Source table must match the parent view root table when linking views.");
            }
            parentViewId = parent.getId();
            for (View ancestor : resolveChain(projectId, parent)) {
                chainQueries.add(readQuery(ancestor.getQueryJson()));
            }
        } else {
            if (!StringUtils.hasText(request.getSourceTableId())) {
                throw new IllegalArgumentException("Source table is required when no parent view is set.");
            }
            rootTable = tableService.getEntityById(request.getSourceTableId().trim(), projectId);
        }
        chainQueries.add(storedQuery);
        String compiledSql = tableQueryExecutionService.compileChain(rootTable, projectId, chainQueries, authentication);

        View view = View.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .type(request.getType())
                .sourceTableId(rootTable.getId())
                .parentViewId(parentViewId)
                .stepsJson(writeSteps(steps))
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
        List<ViewTransformationStepDto> steps = normalizeSteps(request.getSteps());
        TableQueryRequest storedQuery = normalizeStoredQuery(request.getQuery());

        Table rootTable;
        List<TableQueryRequest> chainQueries = new ArrayList<>();
        if (StringUtils.hasText(view.getParentViewId())) {
            View parent = getEntityById(projectId, view.getParentViewId());
            rootTable = tableService.getEntityById(parent.getSourceTableId(), projectId);
            for (View ancestor : resolveChain(projectId, parent)) {
                chainQueries.add(readQuery(ancestor.getQueryJson()));
            }
        } else {
            if (!StringUtils.hasText(request.getSourceTableId())) {
                throw new IllegalArgumentException("Source table is required.");
            }
            rootTable = tableService.getEntityById(request.getSourceTableId().trim(), projectId);
        }
        chainQueries.add(storedQuery);
        String compiledSql = tableQueryExecutionService.compileChain(rootTable, projectId, chainQueries, authentication);

        view.setName(request.getName().trim());
        view.setDescription(normalizeDescription(request.getDescription()));
        view.setType(request.getType());
        view.setSourceTableId(rootTable.getId());
        view.setStepsJson(writeSteps(steps));
        view.setQueryJson(writeQuery(storedQuery));
        view.setCompiledSql(compiledSql);

        return toDto(viewRepository.save(view));
    }

    @Transactional
    public void delete(String projectId, String viewId, Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        View view = getEntityById(projectId, viewId);
        if (viewRepository.existsByParentViewId(view.getId())) {
            throw new IllegalArgumentException("This view cannot be deleted because other views are linked to it.");
        }
        viewRepository.delete(view);
    }

    @Transactional(readOnly = true)
    public TableQueryResponse query(String projectId,
                                    String viewId,
                                    TableQueryRequest runtimeRequest,
                                    Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        View view = getEntityById(projectId, viewId);
        Table rootTable = tableService.getEntityById(view.getSourceTableId(), projectId);
        List<TableQueryRequest> chainQueries = new ArrayList<>();
        for (View chainView : resolveChain(projectId, view)) {
            chainQueries.add(readQuery(chainView.getQueryJson()));
        }
        TableQueryRequest lastQuery = chainQueries.get(chainQueries.size() - 1);
        lastQuery.setPage(runtimeRequest == null ? null : runtimeRequest.getPage());
        lastQuery.setSize(runtimeRequest == null ? null : runtimeRequest.getSize());
        assertRuntimeOverridesOnly(runtimeRequest);
        return tableQueryExecutionService.queryChain(rootTable, projectId, chainQueries, authentication);
    }

    @Transactional(readOnly = true)
    public TableQueryResponse preview(String projectId,
                                      ViewPreviewRequest previewRequest,
                                      Authentication authentication) {
        projectAccessService.checkCanAdministrateProject(projectId, authentication);
        if (previewRequest == null) {
            throw new IllegalArgumentException("Preview request is required.");
        }
        TableQueryRequest runtimeQuery = previewRequest.getQuery() == null
                ? TableQueryRequest.builder().build()
                : previewRequest.getQuery();
        if (StringUtils.hasText(previewRequest.getParentViewId())) {
            View parent = getEntityById(projectId, previewRequest.getParentViewId().trim());
            Table rootTable = tableService.getEntityById(parent.getSourceTableId(), projectId);
            List<TableQueryRequest> chainQueries = new ArrayList<>();
            for (View ancestor : resolveChain(projectId, parent)) {
                chainQueries.add(readQuery(ancestor.getQueryJson()));
            }
            chainQueries.add(runtimeQuery);
            return tableQueryExecutionService.queryChain(rootTable, projectId, chainQueries, authentication);
        }
        if (!StringUtils.hasText(previewRequest.getSourceTableId())) {
            throw new IllegalArgumentException("Source table is required when no parent view is set.");
        }
        Table rootTable = tableService.getEntityById(previewRequest.getSourceTableId().trim(), projectId);
        return tableQueryExecutionService.queryChain(rootTable, projectId, List.of(runtimeQuery), authentication);
    }

    private List<View> resolveChain(String projectId, View leaf) {
        LinkedList<View> chain = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        View current = leaf;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new IllegalStateException("Circular view reference detected.");
            }
            chain.addFirst(current);
            current = StringUtils.hasText(current.getParentViewId())
                    ? getEntityById(projectId, current.getParentViewId())
                    : null;
        }
        return chain;
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

    private List<ViewTransformationStepDto> normalizeSteps(List<ViewTransformationStepDto> steps) {
        if (steps == null) {
            return List.of();
        }
        List<ViewTransformationStepDto> normalized = new ArrayList<>();
        for (ViewTransformationStepDto step : steps) {
            if (step == null) {
                continue;
            }
            String type = step.getType() == null ? "" : step.getType().trim().toUpperCase(Locale.ROOT);
            if (!STEP_TYPES.contains(type)) {
                throw new IllegalArgumentException("Unsupported view step type: " + step.getType());
            }
            step.setType(type);
            if (!StringUtils.hasText(step.getLabel())) {
                step.setLabel(defaultStepLabel(step));
            }
            if (!StringUtils.hasText(step.getId())) {
                step.setId(UUID.randomUUID().toString());
            }
            normalized.add(step);
        }
        return normalized;
    }

    private String defaultStepLabel(ViewTransformationStepDto step) {
        return switch (step.getType()) {
            case "FILTER" -> "Filter";
            case "RENAME" -> "Rename columns";
            case "DROP" -> "Remove columns";
            case "REORDER" -> "Reorder columns";
            case "COLUMNS" -> "Edit columns";
            case "SORT" -> "Sort";
            default -> step.getType();
        };
    }

    private TableQueryRequest normalizeStoredQuery(TableQueryRequest request) {
        TableQueryRequest effective = request == null ? TableQueryRequest.builder().build() : request;
        return TableQueryRequest.builder()
                .selectedColumns(effective.getSelectedColumns())
                .distinct(effective.getDistinct())
                .filters(effective.getFilters())
                .aggregations(effective.getAggregations())
                .sorts(effective.getSorts())
                .page(null)
                .size(null)
                .build();
    }

    private void assertRuntimeOverridesOnly(TableQueryRequest runtimeRequest) {
        if (runtimeRequest != null && (runtimeRequest.getSelectedColumns() != null
                || runtimeRequest.getDistinct() != null
                || runtimeRequest.getFilters() != null
                || runtimeRequest.getSorts() != null
                || runtimeRequest.getAggregations() != null)) {
            throw new IllegalArgumentException("Only page and size overrides are supported when querying a saved view.");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }

    private String writeQuery(TableQueryRequest query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to serialize the view query.", exception);
        }
    }

    private TableQueryRequest readQuery(String queryJson) {
        try {
            return objectMapper.readValue(queryJson, TableQueryRequest.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to deserialize the view query.", exception);
        }
    }

    private String writeSteps(List<ViewTransformationStepDto> steps) {
        try {
            return objectMapper.writeValueAsString(steps == null ? List.of() : steps);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to serialize the view steps.", exception);
        }
    }

    private List<ViewTransformationStepDto> readSteps(View view) {
        if (StringUtils.hasText(view.getStepsJson())) {
            try {
                return objectMapper.readValue(view.getStepsJson(), new TypeReference<List<ViewTransformationStepDto>>() {
                });
            } catch (JacksonException exception) {
                throw new IllegalStateException("Unable to deserialize the view steps.", exception);
            }
        }
        return deriveStepsFromQuery(readQuery(view.getQueryJson()));
    }

    private List<ViewTransformationStepDto> deriveStepsFromQuery(TableQueryRequest query) {
        List<ViewTransformationStepDto> steps = new ArrayList<>();
        if (query.getFilters() != null) {
            query.getFilters().forEach(group -> steps.add(ViewTransformationStepDto.builder()
                    .id(UUID.randomUUID().toString())
                    .type("FILTER")
                    .label("Filter")
                    .filter(group)
                    .build()));
        }
        if (query.getSorts() != null) {
            for (SortDto sort : query.getSorts()) {
                steps.add(ViewTransformationStepDto.builder()
                        .id(UUID.randomUUID().toString())
                        .type("SORT")
                        .label("Sort")
                        .column(sort.getColumn())
                        .direction(sort.getDirection())
                        .build());
            }
        }
        return steps;
    }

    private ViewDto toDto(View view) {
        return ViewDto.builder()
                .id(view.getId())
                .name(view.getName())
                .description(normalizeDescription(view.getDescription()))
                .type(view.getType())
                .sourceTableId(view.getSourceTableId())
                .parentViewId(view.getParentViewId())
                .query(readQuery(view.getQueryJson()))
                .steps(readSteps(view))
                .build();
    }
}
