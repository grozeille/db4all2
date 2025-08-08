package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.LoginRequest;
import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/v2/setup")
@Slf4j
@Tag(name = "Setup", description = "Endpoints for application initialization")
@RequiredArgsConstructor
public class SetupController {

    private final UserService userService;

    @Operation(summary = "Check initialization", description = "Checks if the application has at least one user. This is used by the UI to determine if the setup page should be shown.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is initialized and ready.", content = @Content(schema = @Schema(implementation = Object.class))),
        @ApiResponse(responseCode = "404", description = "Application is not initialized.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> checkInitialization() {
        if (!userService.isInitialized()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No user exists. Initialization required.", "INITIALIZATION_REQUIRED"));
        }
        return ResponseEntity.ok(Collections.emptyMap());
    }

    @Operation(summary = "Initialize application", description = "Creates the first super admin user. This can only be done once.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Initialization successful, the first admin user has been created.", content = @Content(schema = @Schema(implementation = Object.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request, for example, if the application is already initialized.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> initialize(@ModelAttribute LoginRequest req) {
        try {
            userService.createInitialAdmin(req.getUsername(), req.getPassword());
            return ResponseEntity.ok(Collections.emptyMap());
        } catch (IllegalStateException e) {
            log.error("Initialization attempt failed: already initialized.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Initialization already done."));
        } catch (IllegalArgumentException e) {
            log.error("Initialization attempt failed: invalid argument.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
