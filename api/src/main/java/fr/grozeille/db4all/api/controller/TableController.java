package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.TableCreationRequest;
import fr.grozeille.db4all.api.dto.TableUpdateRequest;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v2/projects/{projectId}/tables")
@Tag(name = "Table Management", description = "APIs for managing tables within a project.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@Slf4j
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @Operation(summary = "List all tables in a project", description = "Retrieves a paginated list of tables for a given project, optionally filtered by a search term.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of tables."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Table>> getAll(@PathVariable String projectId, @RequestParam(required = false) String search, Pageable pageable) {
        Page<Table> tables = tableService.findAll(projectId, search, pageable);
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
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String tableId) {
        Optional<Table> opt = tableService.findById(tableId, projectId);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new ErrorResponse("Table not found")));
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
    public ResponseEntity<?> create(@PathVariable String projectId, @RequestBody TableCreationRequest request) {
        try {
            if (request.getName() == null || request.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            Table saved = tableService.create(projectId, request.getName(), request.getDescription());
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during table creation: {}", request.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Table name already exists"));
        } catch (Exception e) {
            log.error("Error during table creation", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
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
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String tableId, @RequestBody TableUpdateRequest request) {
        try {
            if (!tableService.exists(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Table not found"));
            }
            if (request.getName() == null || request.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            Table saved = tableService.update(tableId, projectId, request.getName(), request.getDescription());
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during table update: {}", request.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Table name already exists"));
        } catch (Exception e) {
            log.error("Error while updating table {} from project {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @Operation(summary = "Delete a table", description = "Deletes a table and all of its associated data by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Table deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or Project with the specified ID not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{tableId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String tableId) {
        try {
            if (!tableService.exists(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Table not found"));
            }
            tableService.delete(tableId, projectId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error while deleting table {} from project {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }
}
