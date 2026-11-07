package fr.grozeille.db4all.api.dto;

import fr.grozeille.db4all.api.model.DatasourceScanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceScanRequest {
    private String filePath;
    private DatasourceScanType scanType;
}
