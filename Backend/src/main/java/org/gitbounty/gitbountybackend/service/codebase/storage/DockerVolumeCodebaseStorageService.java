package org.gitbounty.gitbountybackend.service.codebase.storage;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
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
        Path repositoryPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();
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
                        deleteWithRetry(path);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to clean up repository directory", e);
                    }
                });
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete repository", e);
        }

        // On WSL2/DrvFs (9P), the VFS dentry cache can lag behind the syscall return,
        // so Files.exists may still return true immediately after a successful delete.
        // Poll briefly to let the cache invalidation propagate before returning.
        for (int i = 0; i < 10 && Files.exists(repositoryPath); i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // On Windows (and WSL2 over NTFS), external agents such as OneDrive sync or
    // AV software can briefly hold a handle on newly-created pack files, causing
    // AccessDeniedException on delete. Retry with exponential back-off to let the
    // handle be released (OneDrive typically finishes within 1-2 s).
    private static void deleteWithRetry(Path path) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (AccessDeniedException e) {
                if (attempt >= 4) throw e;  // 5 attempts: waits of 100/200/400/800 ms
                try {
                    Thread.sleep(100L << attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}

