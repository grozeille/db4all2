package fr.grozeille.db4all.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One data-wrangling transformation step stored on a view.
 * The meaningful payload depends on {@code type}:
 * <ul>
 *   <li>FILTER: {@code filter} holds the filter group applied by this step.</li>
 *   <li>RENAME: {@code column} is the current name, {@code newName} the new name.</li>
 *   <li>DROP: {@code column} is the removed column.</li>
 *   <li>REORDER: {@code columns} is the full ordered column list after this step.</li>
 *   <li>SORT: {@code sorts} is the full sort list after this step (empty clears the sort).
 *   Legacy single-column form ({@code column} + {@code direction}) is also accepted.</li>
 *   <li>COLUMNS: {@code selections} is the merged net result of consecutive
 *   rename/remove/reorder operations (ordered entries with optional aliases).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewTransformationStepDto {
    private String id;
    private String type;
    private String label;
    private FilterGroupDto filter;
    private String column;
    private String newName;
    private List<String> columns;
    private String direction;
    private List<SortDto> sorts;
    private List<SelectedColumnDto> selections;
}
