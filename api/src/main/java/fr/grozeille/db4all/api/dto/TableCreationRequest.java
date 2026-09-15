package fr.grozeille.db4all.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import fr.grozeille.db4all.api.model.TableSourceKind;
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
public class TableCreationRequest {
    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String datasourceId;

    @NotNull
    private TableSourceKind sourceKind;

    @NotNull
    private JsonNode configuration;
}
