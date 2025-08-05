package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public Page<Project> findAll(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return projectRepository.findByNameContainingIgnoreCase(search, pageable);
        }
        return projectRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(String id) {
        return projectRepository.findById(id);
    }

    @Transactional
    public Project create(String name, String description) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Project name is required.");
        }

        Project project = new Project();
        project.setId(UUID.randomUUID().toString());
        project.setName(name);
        project.setDescription(description);
        // The administrator flag is not set here, assuming default behavior

        return projectRepository.save(project);
    }

    @Transactional
    public Project update(String id, String name, String description) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Project name is required.");
        }

        project.setName(name);
        project.setDescription(description);

        return projectRepository.save(project);
    }

    @Transactional
    public void delete(String id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
    }
}
