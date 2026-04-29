package fr.grozeille.db4all.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;

@Entity
@jakarta.persistence.Table(name = "tables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false, name = "project_id")
    private String projectId;

    @Column(name = "datasource_id")
    private String datasourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind")
    private TableSourceKind sourceKind;

    @Lob
    @Column(name = "configuration_json")
    private String configurationJson;
}
