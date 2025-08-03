package fr.grozeille.dataprep.api.dto;

import lombok.Data;

@Data
public class InitRequest {
    private String email;
    private String password;
    private String passwordConfirm;
}
