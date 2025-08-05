package fr.grozeille.db4all.api.controller;

import fr.grozeille.db4all.api.dto.ProjectCreationRequest;
import fr.grozeille.db4all.api.dto.ProjectUpdateRequest;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<Page<Project>> getAll(@RequestParam(required = false) String search, Pageable pageable) {
        Page<Project> projects = projectService.findAll(search, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getById(@PathVariable String id) {
        return projectService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestBody ProjectCreationRequest request) {
        Project createdProject = projectService.create(request.getName(), request.getDescription());
        return ResponseEntity.ok(createdProject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> update(@PathVariable String id, @RequestBody ProjectUpdateRequest request) {
        Project updatedProject = projectService.update(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
