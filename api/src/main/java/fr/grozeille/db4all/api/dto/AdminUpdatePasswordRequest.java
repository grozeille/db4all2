package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminUpdatePasswordRequest {
    @Schema(description = "The new password for the user.", format = "password", required = true)
    private String newPassword;
}
