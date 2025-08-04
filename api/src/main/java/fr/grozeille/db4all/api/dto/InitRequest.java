package fr.grozeille.db4all.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InitRequest {
    private String email;
    @Schema(format = "password")
    private String password;
    @Schema(format = "password")
    private String passwordConfirm;
}
