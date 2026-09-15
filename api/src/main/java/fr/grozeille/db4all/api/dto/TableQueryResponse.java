package fr.grozeille.db4all.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableQueryResponse {
    private List<ColumnDefinitionDto> columns;
    private List<Map<String, Object>> rows;
}