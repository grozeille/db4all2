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
public class FilterConditionDto {
    private String column;
    private String operator;
    private Object value;
    private List<Object> values;
    private Boolean ignoreCase;
}