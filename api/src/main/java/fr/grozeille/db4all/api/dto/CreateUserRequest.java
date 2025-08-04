package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateUserRequest {

    @Schema(description = "The login for the new user.", required = true)
    private String login;

    @Schema(description = "The password for the new user.", format = "password", required = true)
    private String password;

    @Schema(description = "Set to true to grant administrator privileges.", defaultValue = "false")
    private boolean superAdmin;
}
