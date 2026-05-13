package org.gitbounty.gitbountybackend.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.junit.jupiter.api.Test;

class GitServletConfigurationTests {

    @Test
    void repositoryResolverThrowsWhenRepositoryDoesNotExist() throws Exception {
        GitServletConfiguration configuration = new GitServletConfiguration();
        Path repositoriesRoot = Files.createTempDirectory("gitbounty-repositories-root-");

        try {
            RepositoryResolver<?> resolver = configuration.repositoryResolver(repositoriesRoot);

            assertThatThrownBy(() -> resolver.open(null, "missing.git"))
                .isInstanceOf(RepositoryNotFoundException.class);
        } finally {
            try (var paths = Files.walk(repositoriesRoot)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }
    }
}

