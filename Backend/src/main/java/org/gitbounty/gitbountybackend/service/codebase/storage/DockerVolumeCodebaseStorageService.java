package org.gitbounty.gitbountybackend.service.codebase.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DockerVolumeCodebaseStorageService implements CodebaseStorageService {

    private final Path repositoriesRoot;

    public DockerVolumeCodebaseStorageService(Path resolveRepositoriesRoot) {
        this.repositoriesRoot = resolveRepositoriesRoot;
    }

    @Override
    public void createRepository(String repositoryName) {
        Path repositoryPath = resolveRepositoryPath(repositoryName);

        if (Files.exists(repositoryPath)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Repository directory already exists: " + repositoryName
            );
        }

        try {
            Files.createDirectories(repositoriesRoot);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        }

        try (Git git = Git.init().setBare(true).setDirectory(repositoryPath.toFile()).call()) {
            // Touch repository to avoid an empty try block while still relying on JGit resource cleanup.
            git.getRepository();
        } catch (GitAPIException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create repository", e);
        } catch (RuntimeException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw e;
        }
    }

    @Override
    public void deleteRepository(String repositoryName) {
        Path repositoryPath = resolveRepositoryPath(repositoryName);
        cleanupRepositoryDirectory(repositoryPath);
    }

    private Path resolveRepositoryPath(String repositoryName) {
        Path repositoryPath = repositoriesRoot.resolve(repositoryName).normalize();
        if (!repositoryPath.startsWith(repositoriesRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid repository name: " + repositoryName);
        }
        return repositoryPath;
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete repository", e);
        }
    }
}

