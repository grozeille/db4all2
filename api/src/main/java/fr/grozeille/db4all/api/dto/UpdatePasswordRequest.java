package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @Schema(description = "The user's current password. Required when a user changes their own password.", format = "password")
    private String oldPassword;

    @Schema(description = "The new password.", format = "password", required = true)
    private String newPassword;
}
