package fr.grozeille.db4all.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import fr.grozeille.db4all.api.model.TableSourceKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDto {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private TableSourceKind sourceKind;
    private JsonNode configuration;
}