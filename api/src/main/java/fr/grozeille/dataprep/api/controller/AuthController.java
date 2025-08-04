package fr.grozeille.dataprep.api.controller;

import fr.grozeille.dataprep.api.dto.LoginRequest;
import fr.grozeille.dataprep.api.dto.LoginResponse;
import fr.grozeille.dataprep.api.model.User;
import fr.grozeille.dataprep.api.dto.ErrorResponse;
import fr.grozeille.dataprep.api.security.JwtUtil;
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

import java.util.Optional;

@RestController
@RequestMapping("/v2/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for application authentication")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates a user and returns a JWT token.
     * @return 200 OK with token, 401 UNAUTHORIZED with error message otherwise
     */
    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> login(@ModelAttribute LoginRequest request) {
        Optional<User> userOpt = userService.authenticate(request.getUsername(), request.getPassword());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials."));
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail(), user.isSuperAdmin());
        return ResponseEntity.ok(new LoginResponse(token, user.getEmail()));
    }
}
