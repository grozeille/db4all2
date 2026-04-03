package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.ProjectAdministratorDto;
import fr.grozeille.db4all.api.dto.ProjectCreationRequest;
import fr.grozeille.db4all.api.dto.ProjectDto;
import fr.grozeille.db4all.api.dto.ProjectUpdateRequest;
import fr.grozeille.db4all.api.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/projects")
@Tag(name = "Project Management", description = "APIs for managing projects.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "List all projects", description = "Retrieves a paginated list of projects, optionally filtered by a search term.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of projects."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProjectDto>> getAll(@RequestParam(required = false) String search, @NonNull Pageable pageable, Authentication authentication) {
        Page<ProjectDto> projects = projectService.findAll(search, pageable, authentication);
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
    public ResponseEntity<ProjectDto> getById(@PathVariable @NonNull String id, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.findById(id, authentication));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Project not found:")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @Operation(summary = "Create a new project", description = "Creates a new project with a name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project created successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., missing name.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(@RequestBody ProjectCreationRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.create(request.getName(), request.getDescription(), authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_NAME_ALREADY_USED));
        }
    }

    @Operation(summary = "Update a project", description = "Updates an existing project's name and description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project updated successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> update(@PathVariable @NonNull String id, @RequestBody ProjectUpdateRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.update(id, request.getName(), request.getDescription(), authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_NAME_ALREADY_USED));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Project not found:")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @Operation(summary = "Delete a project", description = "Deletes a project and all of its associated data by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project with the specified ID not found.", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> delete(@PathVariable @NonNull String id, Authentication authentication) {
        try {
            projectService.delete(id, authentication);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Project not found:")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @Operation(summary = "List project administrators", description = "Returns the administrators for a project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved project administrators.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProjectAdministratorDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/administrators")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAdministrators(@PathVariable @NonNull String id, Authentication authentication) {
        try {
            List<ProjectAdministratorDto> administrators = projectService.getAdministrators(id, authentication);
            return ResponseEntity.ok(administrators);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        }
    }

    @Operation(summary = "List users available as project administrators", description = "Returns users who are not yet administrators of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved available users."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/administrators/available-users")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailableAdministrators(@PathVariable @NonNull String id, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.getAvailableAdministrators(id, authentication));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        }
    }

    @Operation(summary = "Add a project administrator", description = "Adds a user as administrator of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrator added successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or project not found.", content = @Content)
    })
    @PostMapping("/{id}/administrators/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addAdministrator(@PathVariable @NonNull String id, @PathVariable @NonNull String email, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.addAdministrator(id, email, authentication));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ADMIN_ALREADY_EXISTS));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("User not found:")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(e.getMessage(), ErrorResponse.USER_NOT_FOUND));
            }
            if (e.getMessage() != null && e.getMessage().startsWith("Project not found:")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Failed to add administrator {} to project {}", email, id, e);
            throw e;
        }
    }

    @Operation(summary = "Remove a project administrator", description = "Removes a user from the administrators of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrator removed successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or project not found.", content = @Content)
    })
    @DeleteMapping("/{id}/administrators/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeAdministrator(@PathVariable @NonNull String id, @PathVariable @NonNull String email, Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.removeAdministrator(id, email, authentication));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_LAST_ADMIN_REMOVAL_FORBIDDEN));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ADMIN_SELF_REMOVAL_FORBIDDEN));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PROJECT_ACCESS_FORBIDDEN));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("User not found:")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(e.getMessage(), ErrorResponse.USER_NOT_FOUND));
            }
            if (e.getMessage() != null && e.getMessage().startsWith("Project not found:")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Failed to remove administrator {} from project {}", email, id, e);
            throw e;
        }
    }
}
