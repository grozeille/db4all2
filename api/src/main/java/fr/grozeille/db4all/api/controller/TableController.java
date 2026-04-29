package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.TableCreationRequest;
import fr.grozeille.db4all.api.dto.TableDto;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.dto.TableQueryResponse;
import fr.grozeille.db4all.api.dto.TableUpdateRequest;
import fr.grozeille.db4all.api.service.TableService;
import fr.grozeille.db4all.api.service.TableQueryExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/projects/{projectId}/tables")
@Tag(name = "Table Management", description = "APIs for managing tables within a project.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@Slf4j
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final TableQueryExecutionService tableQueryExecutionService;

    @Operation(summary = "List all tables in a project", description = "Retrieves a paginated list of tables for a given project, optionally filtered by a search term.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of tables."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TableDto>> getAll(@PathVariable String projectId,
                                                 @RequestParam(required = false) String search,
                                                 Pageable pageable,
                                                 Authentication authentication) {
        Page<TableDto> tables = tableService.findAll(projectId, search, pageable, authentication);
        return ResponseEntity.ok(tables);
    }

    @Operation(summary = "Get a table by ID", description = "Retrieves a single table by its unique ID within a given project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the table."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or Project with the specified ID not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{tableId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableDto> getById(@PathVariable String projectId,
                                            @PathVariable String tableId,
                                            Authentication authentication) {
        return ResponseEntity.ok(tableService.findById(tableId, projectId, authentication));
    }

    @Operation(summary = "Create a new table", description = "Creates a new table with a name and description within a given project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., missing name or name already exists.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableDto> create(@PathVariable String projectId,
                                           @Valid @RequestBody TableCreationRequest request,
                                           Authentication authentication) {
        TableDto saved = tableService.create(projectId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update a table", description = "Updates an existing table's name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., missing name or name already exists.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or Project with the specified ID not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{tableId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableDto> update(@PathVariable String projectId,
                                           @PathVariable String tableId,
                                           @Valid @RequestBody TableUpdateRequest request,
                                           Authentication authentication) {
        return ResponseEntity.ok(tableService.update(tableId, projectId, request, authentication));
    }

    @Operation(summary = "Delete a table", description = "Deletes a table and all of its associated data by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Table deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or Project with the specified ID not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{tableId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String projectId,
                                       @PathVariable String tableId,
                                       Authentication authentication) {
        tableService.delete(tableId, projectId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Query a table", description = "Runs the JSON query DSL against a table through DuckDB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Query executed successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TableQueryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or datasource not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Query is semantically invalid.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{tableId}/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableQueryResponse> query(@PathVariable String projectId,
                                                    @PathVariable String tableId,
                                                    @RequestBody(required = false) TableQueryRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(tableQueryExecutionService.queryTable(projectId, tableId, request, authentication));
    }
}
