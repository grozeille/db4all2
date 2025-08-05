package fr.grozeille.db4all.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectCreationRequest {
    private String name;
    private String description;
}
