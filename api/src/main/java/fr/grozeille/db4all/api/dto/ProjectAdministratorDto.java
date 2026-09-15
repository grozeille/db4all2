package fr.grozeille.db4all.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAdministratorDto {
    private String email;
    private boolean currentUser;
}