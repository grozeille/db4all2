package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;

    public Page<Table> findAll(String projectId, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return tableRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, search, pageable);
        } else {
            return tableRepository.findByProjectId(projectId, pageable);
        }
    }

    public Optional<Table> findById(String tableId, String projectId) {
        return tableRepository.findByIdAndProjectId(tableId, projectId);
    }

    public Table create(String projectId, String name, String description) {
        Table table = Table.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .name(name)
                .description(description)
                .build();
        return tableRepository.save(table);
    }

    public Table update(String tableId, String projectId, String name, String description) {
        Table table = Table.builder()
                .id(tableId)
                .projectId(projectId)
                .name(name)
                .description(description)
                .build();
        return tableRepository.save(table);
    }

    public void delete(String tableId, String projectId) {
        if (!tableRepository.existsByIdAndProjectId(tableId, projectId)) {
            // Or throw a custom exception
            return;
        }
        tableRepository.deleteById(tableId);
    }

    public boolean exists(String tableId, String projectId) {
        return tableRepository.existsByIdAndProjectId(tableId, projectId);
    }
}
