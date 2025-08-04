package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    @Schema(format = "password")
    private String password;
}
