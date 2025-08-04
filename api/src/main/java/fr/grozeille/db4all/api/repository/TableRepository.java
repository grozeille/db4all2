package fr.grozeille.db4all.api.repository;

import fr.grozeille.db4all.api.model.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<Table, String> {
    Page<Table> findByProjectId(String projectId, Pageable pageable);
    Page<Table> findByProjectIdAndNameContainingIgnoreCase(String projectId, String name, Pageable pageable);
    Optional<Table> findByIdAndProjectId(String id, String projectId);
    boolean existsByIdAndProjectId(String id, String projectId);
}
