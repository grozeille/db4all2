package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.dto.ProjectAdministratorDto;
import fr.grozeille.db4all.api.dto.ProjectDto;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.ProjectRepository;
import fr.grozeille.db4all.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private String normalizeDescription(String description) {
        return description == null ? "" : description;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Current user not found."));
    }

    private boolean isProjectAdministrator(Project project, User user) {
        return user.isSuperAdmin() || project.getAdministrators().stream()
                .anyMatch(administrator -> administrator.getEmail().equals(user.getEmail()));
    }

    private void ensureProjectAdministrator(Project project, User user) {
        if (!isProjectAdministrator(project, user)) {
            throw new AccessDeniedException("You don't have permission to administer this project.");
        }
    }

    private Project getProjectOrThrow(@NonNull String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
    }

    private ProjectDto toProjectDto(Project project, User currentUser, boolean includeAdministrators) {
        List<ProjectAdministratorDto> administrators = includeAdministrators
                ? project.getAdministrators().stream()
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(administrator -> ProjectAdministratorDto.builder()
                        .email(administrator.getEmail())
                        .currentUser(administrator.getEmail().equals(currentUser.getEmail()))
                        .build())
                .toList()
                : List.of();

        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(normalizeDescription(project.getDescription()))
                .administrator(isProjectAdministrator(project, currentUser))
                .administrators(administrators)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> findAll(String search, @NonNull Pageable pageable, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (StringUtils.hasText(search)) {
            return projectRepository.findByNameContainingIgnoreCase(search, pageable)
                    .map(project -> toProjectDto(project, currentUser, false));
        }
        return projectRepository.findAll(pageable)
                .map(project -> toProjectDto(project, currentUser, false));
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(@NonNull String id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(id);
        return toProjectDto(project, currentUser, true);
    }

    @Transactional
    public ProjectDto create(String name, String description, Authentication authentication) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Project name is already used.");
        }

        User currentUser = getCurrentUser(authentication);

        Project project = new Project();
        project.setId(UUID.randomUUID().toString());
        project.setName(name.trim());
        project.setDescription(normalizeDescription(description));
        project.getAdministrators().add(currentUser);

        return toProjectDto(projectRepository.save(project), currentUser, true);
    }

    @Transactional
    public ProjectDto update(@NonNull String id, String name, String description, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(id);
        ensureProjectAdministrator(project, currentUser);

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Project name is already used.");
        }

        project.setName(name.trim());
        project.setDescription(normalizeDescription(description));

        return toProjectDto(projectRepository.save(project), currentUser, true);
    }

    @Transactional
    public void delete(@NonNull String id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(id);
        ensureProjectAdministrator(project, currentUser);
        projectRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProjectAdministratorDto> getAdministrators(@NonNull String projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        ensureProjectAdministrator(project, currentUser);
        return toProjectDto(project, currentUser, true).getAdministrators();
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableAdministrators(@NonNull String projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        ensureProjectAdministrator(project, currentUser);

        List<String> currentAdministrators = project.getAdministrators().stream()
                .map(User::getEmail)
                .toList();

        return userRepository.findAll().stream()
                .map(User::getEmail)
                .filter(email -> !currentAdministrators.contains(email))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional
    public List<ProjectAdministratorDto> addAdministrator(@NonNull String projectId, @NonNull String userEmail, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        ensureProjectAdministrator(project, currentUser);

        User userToAdd = userRepository.findById(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        boolean alreadyAdministrator = project.getAdministrators().stream()
                .anyMatch(administrator -> administrator.getEmail().equals(userEmail));
        if (alreadyAdministrator) {
            throw new IllegalArgumentException("This user is already a project administrator.");
        }

        project.getAdministrators().add(userToAdd);
        return toProjectDto(projectRepository.save(project), currentUser, true).getAdministrators();
    }

    @Transactional
    public List<ProjectAdministratorDto> removeAdministrator(@NonNull String projectId, @NonNull String userEmail, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        ensureProjectAdministrator(project, currentUser);

        if (currentUser.getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You cannot remove yourself from the project administrators.");
        }

        boolean removed = project.getAdministrators().removeIf(administrator -> administrator.getEmail().equals(userEmail));
        if (!removed) {
            throw new RuntimeException("User not found: " + userEmail);
        }
        if (project.getAdministrators().isEmpty()) {
            throw new IllegalStateException("A project must keep at least one administrator.");
        }

        return toProjectDto(projectRepository.save(project), currentUser, true).getAdministrators();
    }
}
