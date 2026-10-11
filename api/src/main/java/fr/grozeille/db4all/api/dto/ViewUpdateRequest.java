package fr.grozeille.db4all.api.dto;

import fr.grozeille.db4all.api.model.ViewType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewUpdateRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private ViewType type;

    private String sourceTableId;

    private List<ViewTransformationStepDto> steps;

    @Valid
    @NotNull
    private TableQueryRequest query;
}
