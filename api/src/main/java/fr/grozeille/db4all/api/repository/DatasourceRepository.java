package fr.grozeille.db4all.api.repository;

import fr.grozeille.db4all.api.model.Datasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasourceRepository extends JpaRepository<Datasource, String> {

    List<Datasource> findByProjectIdOrderByNameAsc(String projectId);

    Optional<Datasource> findByIdAndProjectId(String id, String projectId);

    boolean existsByProjectIdAndNameIgnoreCase(String projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(String projectId, String name, String id);
}