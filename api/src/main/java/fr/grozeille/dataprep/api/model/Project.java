package fr.grozeille.dataprep.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    private Boolean administrator;
}
