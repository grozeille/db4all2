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
@jakarta.persistence.Table(name = "views")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class View {

    @Id
    private String id;

    @Column(nullable = false, name = "project_id")
    private String projectId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViewType type;

    @Column(nullable = false, name = "source_table_id")
    private String sourceTableId;

    @Lob
    @Column(nullable = false, name = "query_json")
    private String queryJson;

    @Lob
    @Column(nullable = false, name = "compiled_sql")
    private String compiledSql;
}