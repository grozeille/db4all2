package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.ProjectCreationRequest;
import fr.grozeille.db4all.api.dto.ProjectUpdateRequest;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/projects")
@Tag(name = "Project Management", description = "APIs for managing projects.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "List all projects", description = "Retrieves a paginated list of projects, optionally filtered by a search term.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of projects."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Project>> getAll(@RequestParam(required = false) String search, Pageable pageable) {
        Page<Project> projects = projectService.findAll(search, pageable);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Get a project by ID", description = "Retrieves a single project by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the project."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Project> getById(@PathVariable String id) {
        return projectService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new project", description = "Creates a new project with a name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., missing name.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Project> create(@RequestBody ProjectCreationRequest request) {
        Project createdProject = projectService.create(request.getName(), request.getDescription());
        return ResponseEntity.ok(createdProject);
    }

    @Operation(summary = "Update a project", description = "Updates an existing project's name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Project> update(@PathVariable String id, @RequestBody ProjectUpdateRequest request) {
        Project updatedProject = projectService.update(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(updatedProject);
    }

    @Operation(summary = "Delete a project", description = "Deletes a project and all of its associated data by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
