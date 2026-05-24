package org.gitbounty.gitbountybackend.service.codebase;

import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

        return trimmedName;
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    public void deleteCodebase(String name) {
        String repositoryName = normalizeRepositoryName(name);

        // remove database record if present
        codebaseRepository.findByName(repositoryName).ifPresent(codebaseRepository::delete);
        codebaseStorageService.deleteRepository(repositoryName);
    }

}







