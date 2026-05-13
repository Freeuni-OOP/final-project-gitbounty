package org.gitbounty.gitbountybackend.service.codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

/**
 * Factory for creating codebases. This is a placeholder for any shared logic or dependencies
 * that may be needed by multiple codebase service implementations in the future.
 */
public abstract class AbstractCodebaseFactory {
    protected final Path repositoryRoot;
    public AbstractCodebaseFactory(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public abstract Codebase persistCodebase(String name, String description, String gitUrl, User owner) throws ResponseStatusException;

}
