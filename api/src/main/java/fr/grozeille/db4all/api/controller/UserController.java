package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.*;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/users")
@Tag(name = "User Management", description = "APIs for managing users.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users", description = "Retrieves a complete list of all users. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of users."),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Create a new user", description = "Creates a new user with the specified roles. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., user already exists.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User newUser = userService.createUser(request.getEmail(), request.getPassword(), request.isSuperAdmin());
        return ResponseEntity.ok(newUser);
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
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., old password does not match.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content)
    })
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateCurrentUserPassword(@RequestBody UpdatePasswordRequest request, Authentication authentication) {
        userService.changeCurrentUserPassword(request.getOldPassword(), request.getNewPassword(), authentication);
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<Void> updateUserPassword(@PathVariable String email, @RequestBody AdminUpdatePasswordRequest request) {
        userService.updateUserPasswordByAdmin(email, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a user's SUPER_ADMIN status", description = "Allows a SUPER_ADMIN to grant or revoke another user's SUPER_ADMIN rights. A user cannot change their own status. Requires SUPER_ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request, e.g., a user trying to change their own status.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, a valid JWT token is required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden, the current user is not a SUPER_ADMIN.", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content)
    })
    @PutMapping("/{email}/superadmin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> updateSuperAdminStatus(@PathVariable String email, @RequestBody UpdateSuperAdminRequest request, Authentication authentication) {
        User updatedUser = userService.updateSuperAdminStatus(email, request.isSuperAdmin(), authentication);
        return ResponseEntity.ok(updatedUser);
    }
}
