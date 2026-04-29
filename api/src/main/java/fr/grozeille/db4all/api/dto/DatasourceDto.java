package fr.grozeille.db4all.api.dto;

import fr.grozeille.db4all.api.model.DatasourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceDto {
    private String id;
    private String name;
    private String description;
    private DatasourceType type;
    private boolean readOnly;
    private LocalFilesystemDatasourceConfigurationDto configuration;
}