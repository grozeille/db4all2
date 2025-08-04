
package fr.grozeille.db4all.api.controller;


import fr.grozeille.db4all.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import java.util.Optional;

import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.repository.ProjectRepository;

@RestController
@RequestMapping("/v2/projects")
@Slf4j
public class ProjectController {
    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping
    public ResponseEntity<Page<Project>> getAll(@RequestParam(required = false) String search, Pageable pageable) {
        Page<Project> projects;
        if (search != null && !search.isEmpty()) {
            projects = projectRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            projects = projectRepository.findAll(pageable);
        }
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            Optional<Project> opt = projectRepository.findById(id);
            if (opt.isPresent()) {
                return ResponseEntity.ok(opt.get());
            } else {
                return ResponseEntity.status(404).body(new ErrorResponse("Project not found"));
            }
        } catch (Exception e) {
            log.error("Error while retrieving project {}", id, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Project project) {
        try {
            if (project.getName() == null || project.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            Project saved = projectRepository.save(project);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during project creation: {}", project.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Project name already exists"));
        } catch (Exception e) {
            log.error("Error during project creation", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Project project) {
        try {
            if (!projectRepository.existsById(id)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Project not found"));
            }
            if (project.getName() == null || project.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Name is required"));
            }
            project.setId(id);
            Project saved = projectRepository.save(project);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflict during project update: {}", project.getName());
            return ResponseEntity.status(400).body(new ErrorResponse("Project name already exists"));
        } catch (Exception e) {
            log.error("Error while updating project {}", id, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            if (!projectRepository.existsById(id)) {
                return ResponseEntity.status(404).body(new ErrorResponse("Project not found"));
            }
            projectRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error while deleting project {}", id, e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

}
