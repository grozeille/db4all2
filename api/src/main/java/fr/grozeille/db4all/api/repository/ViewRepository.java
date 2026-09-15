package fr.grozeille.db4all.api.repository;

import fr.grozeille.db4all.api.model.View;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViewRepository extends JpaRepository<View, String> {

    Page<View> findByProjectId(String projectId, Pageable pageable);

    Page<View> findByProjectIdAndNameContainingIgnoreCase(String projectId, String name, Pageable pageable);

    Optional<View> findByIdAndProjectId(String id, String projectId);

    boolean existsByProjectIdAndNameIgnoreCase(String projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(String projectId, String name, String id);
}