package fr.grozeille.db4all.api.dto;

import lombok.Data;

@Data
public class UserDto {
    private String email;
    private boolean superAdmin;
}
