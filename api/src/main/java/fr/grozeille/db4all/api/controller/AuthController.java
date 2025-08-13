package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.LoginRequest;
import fr.grozeille.db4all.api.dto.LoginResponse;
import fr.grozeille.db4all.api.exceptions.UserNotFoundException;
import fr.grozeille.db4all.api.exceptions.WrongPasswordException;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.security.JwtUtil;
import fr.grozeille.db4all.api.service.UserService;
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
@RequestMapping("/api/v2/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for application authentication")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "User Login", description = "Authenticates a user with email and password, returning a JWT token upon success.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed due to invalid credentials.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/login", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> login(@ModelAttribute LoginRequest request) {
        try {
            User user = userService.authenticate(request.getUsername(), request.getPassword());

            String token = jwtUtil.generateToken(user.getEmail(), user.isSuperAdmin());
            return ResponseEntity.ok(new LoginResponse(token, user.getEmail()));
        } catch (WrongPasswordException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.WRONG_PASSWORD));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
