package fr.grozeille.dataprep.api.controller;

import fr.grozeille.dataprep.api.entity.User;
import fr.grozeille.dataprep.api.repository.UserRepository;
import fr.grozeille.dataprep.api.dto.InitRequest;
import fr.grozeille.dataprep.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.regex.Pattern;
import org.passay.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for application initialization and authentication")
public class AuthController {
    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Check if the application is initialized (at least one user exists).
     * @return 200 OK if initialized, 404 with errorType INITIALIZATION_REQUIRED otherwise
     */
    @Operation(summary = "Check initialization", description = "Checks if the application has at least one user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application initialized"),
        @ApiResponse(responseCode = "404", description = "Initialization required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-initialization")
    public ResponseEntity<?> checkInitialization() {
        if (userRepository.count() == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No user exists. Initialization required.", "INITIALIZATION_REQUIRED"));
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Initialize the application by creating the first super admin user.
     * @param req InitRequest DTO (email, password, passwordConfirm)
     * @return 200 OK if created, 400 BAD REQUEST with error message otherwise
     */
    @Operation(summary = "Initialize application", description = "Creates the first super admin user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Initialization successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/initialize")
    public ResponseEntity<?> initialize(@RequestBody InitRequest req) {
        if (userRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Initialization already done."));
        }
        if (!isPasswordStrong(req.password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Password is not strong enough."));
        }
        if (!req.password.equals(req.passwordConfirm)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Passwords do not match."));
        }
        if (req.email == null || req.email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Email is required."));
        }
        User user = new User();
        user.setEmail(req.email);
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setSuperAdmin(true);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // Use Passay to check password strength
    private boolean isPasswordStrong(String password) {
        if (password == null) return false;
        PasswordValidator validator = new PasswordValidator(
            new LengthRule(8, 64),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(
                new CharacterData() {
                    public String getErrorCode() { return "INSUFFICIENT_DIGIT_OR_SPECIAL"; }
                    public String getCharacters() {
                        return EnglishCharacterData.Digit.getCharacters() + EnglishCharacterData.Special.getCharacters();
                    }
                }, 1
            )
        );
        RuleResult result = validator.validate(new PasswordData(password));
        return result.isValid();
    }

    // DTO classes moved to fr.grozeille.dataprep.api.dto
}
