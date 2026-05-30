package org.gitbounty.gitbountybackend.service.codebase;

import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public Codebase createCodebase(String name, String description, String gitUrl, Principal principal) {
        String repositoryName = normalizeRepositoryName(name);
        User owner = resolveOwner(principal);

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already exists: " + repositoryName);
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
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Repository not found: " + normalizedRepositoryName
            ));
    }

    private User resolveOwner(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return userService.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private String normalizeRepositoryName(String name) {
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }

        String trimmedName = name.trim();
        if (trimmedName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }

        if (trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name must not contain path separators");
        }

        // Disallow any use of the ".git" suffix or substring in repository names. The server
        // manages the bare repository directory names itself and a name containing ".git"
        // could lead to surprising paths or user confusion.
        if (trimmedName.contains(".git")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name must not contain '.git'");
        }

        return trimmedName;
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    @Transactional
    public void deleteCodebase(String name) {
        String repositoryName = normalizeRepositoryName(name);

        // First remove repository files from disk. Doing this before the DB delete
        // avoids a transaction rollback (caused by IO errors) re-creating the DB row
        // and leaving the user delete to fail on FK constraints.
        codebaseStorageService.deleteRepository(repositoryName);

        // Now remove database record. The @OneToMany relationships with CascadeType.ALL
        // on the Codebase entity will automatically delete associated branches and commits.
        codebaseRepository.findByName(repositoryName).ifPresent(cb -> {
            codebaseRepository.delete(cb);
            codebaseRepository.flush();
        });
    }

}







