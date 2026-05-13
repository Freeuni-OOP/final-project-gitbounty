package org.gitbounty.gitbountybackend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final UserRepository userRepository;
    private final Path resolveRepositoriesRoot;

    public CodebaseService(
        CodebaseRepository codebaseRepository,
        UserRepository userRepository,
        Path resolveRepositoriesRoot
    ) {
        this.codebaseRepository = codebaseRepository;
        this.userRepository = userRepository;
        this.resolveRepositoriesRoot = resolveRepositoriesRoot;
    }

    public Codebase createCodebase(String name, String description, String gitUrl, Principal principal) {
        String repositoryName = normalizeRepositoryName(name);
        User owner = resolveOwner(principal);

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already exists: " + repositoryName);
        }

        Path repositoryPath = resolveRepositoriesRoot.resolve(repositoryName).normalize();
        if (!repositoryPath.startsWith(resolveRepositoriesRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid repository name: " + repositoryName);
        }

        if (Files.exists(repositoryPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository directory already exists: " + repositoryName);
        }

        try {
            Files.createDirectories(resolveRepositoriesRoot);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        }

        try (Git bareRepository = Git.init().setBare(true).setDirectory(repositoryPath.toFile()).call()) {
            return codebaseRepository.saveAndFlush(
                new Codebase(repositoryName, normalizeDescription(description), gitUrl, owner)
            );
        } catch (GitAPIException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        } catch (RuntimeException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw e;
        }
    }

    private User resolveOwner(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return userRepository.findByUsername(principal.getName())
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

    private void cleanupRepositoryDirectory(Path repositoryPath) {
        if (!Files.exists(repositoryPath)) {
            return;
        }

        try (var paths = Files.walk(repositoryPath)) {
            paths.sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to clean up repository directory", e);
                    }
                });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to clean up repository directory", e);
        }
    }
}







