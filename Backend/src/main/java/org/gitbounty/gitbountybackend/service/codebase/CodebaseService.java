package org.gitbounty.gitbountybackend.service.codebase;


import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Service;

@Service
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final CodebaseStorageService codebaseStorageService;
    private final UserService userService;

    CodebaseService(
        CodebaseRepository codebaseRepository,
        CodebaseStorageService codebaseStorageService,
        UserService userService) {
        this.codebaseRepository = codebaseRepository;
        this.codebaseStorageService = codebaseStorageService;
        this.userService = userService;
    }


    public Codebase createCodebase(String name, String description, String gitUrl, String userId) { // keycloak Id
        String repositoryName = normalizeRepositoryName(name);
        User owner = userService.findByKeycloakId(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new IllegalStateException("Repository already exists");
        }
            codebaseStorageService.createRepository(repositoryName);

        try {
            return codebaseRepository.saveAndFlush(
                new Codebase(repositoryName, normalizeDescription(description), gitUrl, owner)
            );
        } catch (RuntimeException e) {
            try {
                codebaseStorageService.deleteRepository(repositoryName);
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    public Codebase getCodebase(String repositoryName) {
        String normalizedRepositoryName = normalizeRepositoryName(repositoryName);

        return codebaseRepository.findByName(normalizedRepositoryName)
            .orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + normalizedRepositoryName));
    }

    public CodebaseContentsDTO listCodebaseContents(String repositoryName, String path, String branchName) {
        // Verify existence in database
        // (If you want to ensure the repo is tracked in your system)
        if (!codebaseRepository.existsByName(repositoryName)) {
            throw new CodebaseNotFoundException("Codebase not found: " + repositoryName);
        }

        // Fetch contents from the Git service
        PathContents entry = codebaseStorageService.getPathContents(repositoryName, path, branchName);
        return new CodebaseContentsDTO(entry);
    }

    private String normalizeRepositoryName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Repository name is required");
        }

        String trimmedName = name.trim();
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException( "Repository name is required");
        }

        if (trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName.contains("..")) {
            throw new IllegalArgumentException( "Repository name must not contain path separators");
        }

        if(trimmedName.contains(".git")) {
            throw new IllegalArgumentException( "Repository name must not contain .git");
        }

        return trimmedName;
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    public void deleteCodebase(String name) {
        String repositoryName = normalizeRepositoryName(name);

        // Remove DB record first so S
        // app state stays consistent even if
        // filesystem cleanup encounters issues (e.g. transient file locks).
        // Not wrapped in @Transactional so the DB delete commits immediately
        // and cannot be rolled back by a subsequent filesystem failure.
        codebaseRepository.findByName(repositoryName).ifPresent(codebaseRepository::delete);

        codebaseStorageService.deleteRepository(repositoryName);
    }

    public Codebase findByName(String repositoryName) {
        return codebaseRepository.findByName(repositoryName).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + repositoryName));
    }
    public Codebase findById(Long repositoryId) {
        return codebaseRepository.findById(repositoryId).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: Id = " + repositoryId));
    }

    public Codebase updateCodebase(String repositoryName, UpdateCodebaseCommand command) {
        Codebase codebase = codebaseRepository.findByName(repositoryName)
            .orElseThrow(() -> new CodebaseNotFoundException("Codebase not found with name: " + repositoryName));

        command.applyTo(codebase);
        return codebaseRepository.save(codebase);
    }

}







