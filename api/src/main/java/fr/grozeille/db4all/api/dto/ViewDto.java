package fr.grozeille.db4all.api.dto;

import fr.grozeille.db4all.api.model.ViewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewDto {
    private String id;
    private String name;
    private String description;
    private ViewType type;
    private String sourceTableId;
    private TableQueryRequest query;
}