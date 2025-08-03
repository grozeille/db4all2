
package fr.grozeille.dataprep.api.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import fr.grozeille.dataprep.api.model.Project;
import fr.grozeille.dataprep.api.repository.ProjectRepository;

@RestController
@RequestMapping("/projects")
@Slf4j
public class ProjectController {
    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping
    public List<Project> getAll(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            // TODO: ajouter une vraie recherche (ex: findByNameContaining)
            return projectRepository.findAll().stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase()))
                .toList();
        }
        return projectRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return projectRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(new ErrorMessage("Project not found")));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du projet {}", id, e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Project project) {
        try {
            if (project.getName() == null || project.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Name is required"));
            }
            Project saved = projectRepository.save(project);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflit lors de la création du projet: {}", project.getName());
            return ResponseEntity.status(400).body(new ErrorMessage("Project name already exists"));
        } catch (Exception e) {
            log.error("Erreur lors de la création du projet", e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Project project) {
        try {
            if (!projectRepository.existsById(id)) {
                return ResponseEntity.status(404).body(new ErrorMessage("Project not found"));
            }
            if (project.getName() == null || project.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("Name is required"));
            }
            project.setId(id);
            Project saved = projectRepository.save(project);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Conflit lors de la modification du projet: {}", project.getName());
            return ResponseEntity.status(400).body(new ErrorMessage("Project name already exists"));
        } catch (Exception e) {
            log.error("Erreur lors de la modification du projet {}", id, e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            if (!projectRepository.existsById(id)) {
                return ResponseEntity.status(404).body(new ErrorMessage("Project not found"));
            }
            projectRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du projet {}", id, e);
            return ResponseEntity.status(500).body(new ErrorMessage("Internal server error"));
        }
    }

    // Classe utilitaire pour les messages d'erreur JSON
    public static class ErrorMessage {
        private String message;
        public ErrorMessage(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
