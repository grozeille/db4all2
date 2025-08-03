package fr.grozeille.dataprep.api.repository;

import fr.grozeille.dataprep.api.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    // Optionally add custom queries for search
}
