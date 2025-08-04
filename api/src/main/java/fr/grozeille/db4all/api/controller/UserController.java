package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.CreateUserRequest;
import fr.grozeille.db4all.api.dto.UpdatePasswordRequest;
import fr.grozeille.db4all.api.dto.UpdateSuperAdminRequest;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/users")
@Tag(name = "User Management", description = "APIs for managing users.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all users", description = "Requires SUPER_ADMIN role.")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new user", description = "Requires SUPER_ADMIN role.")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User newUser = userService.createUser(request);
        return ResponseEntity.ok(newUser);
    }

    @DeleteMapping("/{login}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a user", description = "Requires SUPER_ADMIN role.")
    public ResponseEntity<Void> deleteUser(@PathVariable String login) {
        userService.deleteUser(login);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current user's password", description = "Allows an authenticated user to change their own password.")
    public ResponseEntity<Void> updateCurrentUserPassword(@RequestBody UpdatePasswordRequest request, Authentication authentication) {
        userService.changeCurrentUserPassword(request.getOldPassword(), request.getNewPassword(), authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{login}/password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update any user's password", description = "Allows a SUPER_ADMIN to change any user's password. The 'oldPassword' field is ignored.")
    public ResponseEntity<Void> updateUserPassword(@PathVariable String login, @RequestBody UpdatePasswordRequest request) {
        userService.updateUserPasswordByAdmin(login, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{login}/superadmin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update a user's SUPER_ADMIN status", description = "Allows a SUPER_ADMIN to grant or revoke another user's SUPER_ADMIN rights. A user cannot change their own status.")
    public ResponseEntity<User> updateSuperAdminStatus(@PathVariable String login, @RequestBody UpdateSuperAdminRequest request, Authentication authentication) {
        User updatedUser = userService.updateSuperAdminStatus(login, request.isSuperAdmin(), authentication);
        return ResponseEntity.ok(updatedUser);
    }
}
