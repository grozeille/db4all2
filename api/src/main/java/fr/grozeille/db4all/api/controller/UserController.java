package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.*;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.service.UserService;
import fr.grozeille.db4all.api.exceptions.PasswordTooWeakException;
import fr.grozeille.db4all.api.exceptions.UserAlreadyExistsException;
import fr.grozeille.db4all.api.exceptions.WrongPasswordException;
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
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/users")
@Tag(name = "User Management", description = "APIs for managing users.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "passwordFlow")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Get current user", description = "Get information about the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the current user.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(modelMapper.map(user, UserDto.class));
    }


    @Operation(summary = "List all users", description = "Retrieves a complete list of all users. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of users.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserDto> userDtos = users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @Operation(summary = "Create a new user", description = "Creates a new user with the specified roles. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., user already exists or password is too weak.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            User newUser = userService.createUser(request.getEmail(), request.getPassword(), request.isSuperAdmin());
            return ResponseEntity.ok(modelMapper.map(newUser, UserDto.class));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.USER_ALREADY_EXISTS));
        } catch (PasswordTooWeakException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PASSWORD_TOO_WEAK));
        }
    }

    @Operation(summary = "Delete a user", description = "Deletes a user by their email. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content)
    })
    @DeleteMapping("/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update current user's password", description = "Allows an authenticated user to change their own password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., old password does not match or too weak.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUserPassword(@RequestBody UpdatePasswordRequest request, Authentication authentication) {
        try {
            userService.changeCurrentUserPassword(authentication.getName(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.noContent().build();
        } catch (PasswordTooWeakException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PASSWORD_TOO_WEAK));
        } catch (WrongPasswordException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.WRONG_PASSWORD));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), null));
        }
    }

    @Operation(summary = "Update any user's password by admin", description = "Allows a SUPER_ADMIN to change any user's password. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password updated successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content)
    })
    @PutMapping("/{email}/password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateUserPassword(@PathVariable String email, @RequestBody AdminUpdatePasswordRequest request) {
        try {
            userService.updateUserPasswordByAdmin(email, request.getPassword());
            return ResponseEntity.noContent().build();
        } catch (PasswordTooWeakException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), ErrorResponse.PASSWORD_TOO_WEAK));
        }
    }

    @Operation(summary = "Update a user's SUPER_ADMIN status", description = "Allows a SUPER_ADMIN to grant or revoke another user's SUPER_ADMIN rights. A user cannot change their own status. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., a user trying to change their own status.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content)
    })
    @PutMapping("/{email}/superadmin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> updateSuperAdminStatus(@PathVariable String email, @RequestBody UpdateSuperAdminRequest request, Authentication authentication) {
        User updatedUser = userService.updateSuperAdminStatus(email, request.isSuperAdmin(), authentication);
        return ResponseEntity.ok(modelMapper.map(updatedUser, UserDto.class));
    }
}
