package fr.grozeille.db4all.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private boolean administrator;
    private List<ProjectAdministratorDto> administrators;
}