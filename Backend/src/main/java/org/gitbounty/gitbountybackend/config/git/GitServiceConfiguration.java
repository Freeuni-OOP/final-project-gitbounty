package org.gitbounty.gitbountybackend.config.git;

import org.gitbounty.gitbountybackend.util.codebase.LocalRepositoryLockProvider;
import org.gitbounty.gitbountybackend.util.codebase.RepositoryLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitServiceConfiguration {
    @Bean
    public RepositoryLockProvider repositoryLockProvider() {
        // Spring calls this once and caches the result
        return new LocalRepositoryLockProvider();
    }
}
