
package fr.grozeille.dataprep.api.controller;


import fr.grozeille.dataprep.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import fr.grozeille.dataprep.api.model.Table;
import fr.grozeille.dataprep.api.repository.TableRepository;

@RestController
@RequestMapping("/projects/{projectId}/tables")
@Slf4j
public class TableController {
    @Autowired
    private TableRepository tableRepository;

    @GetMapping
    public List<Table> getAll(@PathVariable String projectId, @RequestParam(required = false) String search) {
        List<Table> tables = tableRepository.findByProjectId(projectId);
        if (search != null && !search.isEmpty()) {
            return tables.stream()
                .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(search.toLowerCase()))
                .toList();
        }
        return tables;
    }

    @GetMapping("/{tableId}")
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String tableId) {
        try {
            return tableRepository.findByIdAndProjectId(tableId, projectId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(new ErrorMessage("Table not found")));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la table {} du projet {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @RequestBody Table table) {
        try {
            if (table.getName() == null || table.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Name is required"));
            }
            table.setProjectId(projectId);
            Table saved = tableRepository.save(table);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflit lors de la création de la table: {}", table.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Table name already exists"));
        } catch (Exception e) {
            log.error("Erreur lors de la création de la table", e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    @PutMapping("/{tableId}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String tableId, @RequestBody Table table) {
        try {
            if (!tableRepository.existsByIdAndProjectId(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorMessage("Table not found"));
            }
            if (table.getName() == null || table.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Name is required"));
            }
            table.setId(tableId);
            table.setProjectId(projectId);
            Table saved = tableRepository.save(table);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflit lors de la modification de la table: {}", table.getName());
            return ResponseEntity.status(400).body(new ErrorMessage("Table name already exists"));
        } catch (Exception e) {
            log.error("Erreur lors de la modification de la table {} du projet {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @DeleteMapping("/{tableId}")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String tableId) {
        try {
            if (!tableRepository.existsByIdAndProjectId(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Table not found"));
            }
            tableRepository.deleteById(tableId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la table {} du projet {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }
}
