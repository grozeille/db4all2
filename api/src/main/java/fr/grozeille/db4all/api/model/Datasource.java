package fr.grozeille.db4all.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@jakarta.persistence.Table(name = "datasources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Datasource {

    @Id
    private String id;

    @Column(nullable = false, name = "project_id")
    private String projectId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatasourceType type;

    @Column(nullable = false, name = "read_only")
    private boolean readOnly;

    @Lob
    @Column(nullable = false, name = "configuration_json")
    private String configurationJson;
}