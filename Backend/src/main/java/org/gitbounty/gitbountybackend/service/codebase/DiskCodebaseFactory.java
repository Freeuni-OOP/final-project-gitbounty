package org.gitbounty.gitbountybackend.service.codebase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiskCodebaseFactory extends AbstractCodebaseFactory {
    private final CodebaseRepository codebaseRepository;
    public DiskCodebaseFactory(Path repositoryRoot, CodebaseRepository codebaseRepository) {
        super(repositoryRoot);
        this.codebaseRepository = codebaseRepository;

    }

    @Override
    public Codebase persistCodebase(String repositoryName, String description, String gitUrl, User owner) throws ResponseStatusException {
        // Implementation for creating a codebase on disk would go here.
        Path repositoryPath = this.repositoryRoot.resolve(repositoryName).normalize();
        if (!repositoryPath.startsWith(this.repositoryRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid repository name: " + repositoryName);
        }

        if (Files.exists(repositoryPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository directory already exists: " + repositoryName);
        }

        try {
            Files.createDirectories(this.repositoryRoot);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        }

        try (Git ignored = Git.init().setBare(true).setDirectory(repositoryPath.toFile()).call()) {
            return codebaseRepository.saveAndFlush(
                new Codebase(repositoryName, description, gitUrl, owner)
            );
        } catch (GitAPIException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        } catch (RuntimeException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw e;
        }
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
