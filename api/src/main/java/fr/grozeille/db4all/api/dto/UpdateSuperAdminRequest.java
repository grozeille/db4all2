package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateSuperAdminRequest {
    @Schema(description = "Set to true to grant administrator privileges, false to revoke.", required = true)
    private boolean superAdmin;
}
