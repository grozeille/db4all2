package fr.grozeille.db4all.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalFilesystemDatasourceConfigurationDto {
    @NotBlank
    private String rootPath;
}