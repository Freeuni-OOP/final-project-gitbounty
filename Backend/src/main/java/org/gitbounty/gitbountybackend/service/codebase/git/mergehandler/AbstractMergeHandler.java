package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.lib.Repository;

public abstract class AbstractMergeHandler implements MergeHandler {
    protected final Repository repository;

    protected AbstractMergeHandler(Repository repository) {
        this.repository = repository;
    }
}
