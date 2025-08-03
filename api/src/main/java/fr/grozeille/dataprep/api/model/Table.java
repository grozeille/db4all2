package fr.grozeille.dataprep.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Table {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false)
    private String projectId;
}
