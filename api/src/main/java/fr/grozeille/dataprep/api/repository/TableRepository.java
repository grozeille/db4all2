package fr.grozeille.dataprep.api.repository;

import fr.grozeille.dataprep.api.model.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, String> {
    List<Table> findByProjectId(String projectId);
    Optional<Table> findByIdAndProjectId(String id, String projectId);
    boolean existsByIdAndProjectId(String id, String projectId);
}
