package fr.grozeille.dataprep.api.controller;

import fr.grozeille.dataprep.api.dto.LoginRequest;
import fr.grozeille.dataprep.api.dto.ErrorResponse;
import fr.grozeille.dataprep.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/v2/setup")
@Slf4j
@Tag(name = "Setup", description = "Endpoints for application initialization")
@RequiredArgsConstructor
public class SetupController {

    private final UserService userService;

    @Operation(summary = "Check initialization", description = "Checks if the application has at least one user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application initialized"),
        @ApiResponse(responseCode = "404", description = "Initialization required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> checkInitialization() {
        if (!userService.isInitialized()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No user exists. Initialization required.", "INITIALIZATION_REQUIRED"));
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Initialize application", description = "Creates the first super admin user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Initialization successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> initialize(@ModelAttribute LoginRequest req) {
        try {
            userService.createInitialAdmin(req);
            return ResponseEntity.ok().build();
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
