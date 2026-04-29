package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.TableQueryRequest;
import fr.grozeille.db4all.api.dto.TableQueryResponse;
import fr.grozeille.db4all.api.dto.ViewCreationRequest;
import fr.grozeille.db4all.api.dto.ViewDto;
import fr.grozeille.db4all.api.dto.ViewUpdateRequest;
import fr.grozeille.db4all.api.service.ViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/projects/{projectId}/views")
@Tag(name = "View Management", description = "APIs for managing logical views within a project.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @Operation(summary = "List project views", description = "Returns a paginated list of views for a project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Views retrieved successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ViewDto>> getAll(@PathVariable String projectId,
                                                @RequestParam(required = false) String search,
                                                Pageable pageable,
                                                Authentication authentication) {
        return ResponseEntity.ok(viewService.findAll(projectId, search, pageable, authentication));
    }

    @Operation(summary = "Get a view", description = "Returns one logical view of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "View retrieved successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "View not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{viewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ViewDto> getById(@PathVariable String projectId,
                                           @PathVariable String viewId,
                                           Authentication authentication) {
        return ResponseEntity.ok(viewService.findById(projectId, viewId, authentication));
    }

    @Operation(summary = "Create a view", description = "Creates a logical view from a source table.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "View created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Table or project not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Semantically invalid query.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ViewDto> create(@PathVariable String projectId,
                                          @Valid @RequestBody ViewCreationRequest request,
                                          Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(viewService.create(projectId, request, authentication));
    }

    @Operation(summary = "Update a view", description = "Updates a logical view.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "View updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "View or table not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Semantically invalid query.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{viewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ViewDto> update(@PathVariable String projectId,
                                          @PathVariable String viewId,
                                          @Valid @RequestBody ViewUpdateRequest request,
                                          Authentication authentication) {
        return ResponseEntity.ok(viewService.update(projectId, viewId, request, authentication));
    }

    @Operation(summary = "Delete a view", description = "Deletes a logical view.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "View deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "View not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{viewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String projectId,
                                       @PathVariable String viewId,
                                       Authentication authentication) {
        viewService.delete(projectId, viewId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Query a view", description = "Executes the saved logical view definition.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "View query executed successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TableQueryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "View not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Semantically invalid query.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{viewId}/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableQueryResponse> query(@PathVariable String projectId,
                                                    @PathVariable String viewId,
                                                    @RequestBody(required = false) TableQueryRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(viewService.query(projectId, viewId, request, authentication));
    }
}