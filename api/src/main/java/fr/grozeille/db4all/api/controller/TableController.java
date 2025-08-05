package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ErrorResponse;
import fr.grozeille.db4all.api.dto.TableCreationRequest;
import fr.grozeille.db4all.api.dto.TableUpdateRequest;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v2/projects/{projectId}/tables")
@Slf4j
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    public ResponseEntity<Page<Table>> getAll(@PathVariable String projectId, @RequestParam(required = false) String search, Pageable pageable) {
        Page<Table> tables = tableService.findAll(projectId, search, pageable);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{tableId}")
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String tableId) {
        Optional<Table> opt = tableService.findById(tableId, projectId);
        return opt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new ErrorResponse("Table not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @RequestBody TableCreationRequest request) {
        try {
            if (request.getName() == null || request.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            Table saved = tableService.create(projectId, request.getName(), request.getDescription());
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during table creation: {}", request.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Table name already exists"));
        } catch (Exception e) {
            log.error("Error during table creation", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @PutMapping("/{tableId}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String tableId, @RequestBody TableUpdateRequest request) {
        try {
            if (!tableService.exists(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Table not found"));
            }
            if (request.getName() == null || request.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            Table saved = tableService.update(tableId, projectId, request.getName(), request.getDescription());
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during table update: {}", request.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Table name already exists"));
        } catch (Exception e) {
            log.error("Error while updating table {} from project {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @DeleteMapping("/{tableId}")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String tableId) {
        try {
            if (!tableService.exists(tableId, projectId)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Table not found"));
            }
            tableService.delete(tableId, projectId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error while deleting table {} from project {}", tableId, projectId, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }
}
