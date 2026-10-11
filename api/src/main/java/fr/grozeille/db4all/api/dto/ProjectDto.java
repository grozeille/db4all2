package fr.grozeille.db4all.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private boolean administrator;
    private List<ProjectAdministratorDto> administrators;
}