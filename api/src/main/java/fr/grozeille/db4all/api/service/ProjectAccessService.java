package fr.grozeille.db4all.api.service;

import fr.grozeille.db4all.api.exceptions.ProjectNotFoundException;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.ProjectRepository;
import fr.grozeille.db4all.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public void checkCanAdministrateProject(String projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        boolean administrator = currentUser.isSuperAdmin() || project.getAdministrators().stream()
                .anyMatch(administratorUser -> administratorUser.getEmail().equals(currentUser.getEmail()));
        if (!administrator) {
            throw new AccessDeniedException("You don't have permission to administer this project.");
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Current user not found."));
    }
}