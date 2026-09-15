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
public class TableQueryRequest {
    private List<SelectedColumnDto> selectedColumns;
    private Boolean distinct;
    private List<FilterGroupDto> filters;
    private AggregationRequestDto aggregations;
    private Integer page;
    private Integer size;
}