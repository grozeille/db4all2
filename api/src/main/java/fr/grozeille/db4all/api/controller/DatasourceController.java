package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ConnectionTestResponse;
import fr.grozeille.db4all.api.dto.DatasourceCreationRequest;
import fr.grozeille.db4all.api.dto.DatasourceDto;
import fr.grozeille.db4all.api.dto.DatasourceUpdateRequest;
import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.service.DatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/projects/{projectId}/datasources")
@Tag(name = "Datasource Management", description = "APIs for managing datasources within a project.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;

    @Operation(summary = "List project datasources", description = "Returns the datasources configured for a project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasources retrieved successfully.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DatasourceDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DatasourceDto>> getAll(@PathVariable String projectId, Authentication authentication) {
        return ResponseEntity.ok(datasourceService.findAll(projectId, authentication));
    }

    @Operation(summary = "Get a datasource", description = "Returns one datasource of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource retrieved successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatasourceDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{datasourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DatasourceDto> getById(@PathVariable String projectId,
                                                 @PathVariable String datasourceId,
                                                 Authentication authentication) {
        return ResponseEntity.ok(datasourceService.findById(projectId, datasourceId, authentication));
    }

    @Operation(summary = "Create a datasource", description = "Creates a datasource for the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Datasource created successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatasourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DatasourceDto> create(@PathVariable String projectId,
                                                @Valid @RequestBody DatasourceCreationRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(datasourceService.create(projectId, request, authentication));
    }

    @Operation(summary = "Update a datasource", description = "Updates an existing datasource of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource updated successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DatasourceDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{datasourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DatasourceDto> update(@PathVariable String projectId,
                                                @PathVariable String datasourceId,
                                                @Valid @RequestBody DatasourceUpdateRequest request,
                                                Authentication authentication) {
        return ResponseEntity.ok(datasourceService.update(projectId, datasourceId, request, authentication));
    }

    @Operation(summary = "Delete a datasource", description = "Deletes an existing datasource of the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Datasource deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{datasourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String projectId,
                                       @PathVariable String datasourceId,
                                       Authentication authentication) {
        datasourceService.delete(projectId, datasourceId, authentication);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Test a datasource connection", description = "Checks that the datasource configuration can be used by the application.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datasource tested successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConnectionTestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{datasourceId}/test-connection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConnectionTestResponse> testConnection(@PathVariable String projectId,
                                                                 @PathVariable String datasourceId,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(datasourceService.testConnection(projectId, datasourceId, authentication));
    }
}