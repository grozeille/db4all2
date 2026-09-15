package fr.grozeille.db4all.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationFunctionDto {
    private String column;
    private String function;
    private String alias;
}