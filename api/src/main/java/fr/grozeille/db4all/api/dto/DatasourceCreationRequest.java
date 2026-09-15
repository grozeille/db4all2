package fr.grozeille.db4all.api.dto;

import fr.grozeille.db4all.api.model.DatasourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceCreationRequest {
    @NotBlank
    private String name;

    private String description;

    @Builder.Default
    private boolean readOnly = true;

    @NotNull
    private DatasourceType type;

    @Valid
    @NotNull
    private LocalFilesystemDatasourceConfigurationDto configuration;
}