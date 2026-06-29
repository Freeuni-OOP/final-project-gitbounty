package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;

public abstract class AbstractMergeHandler implements MergeHandler {
    protected final Repository repository;
    protected final PersonIdent mergerUserId;

    protected AbstractMergeHandler(Repository repository, PersonIdent mergerUserId) {
        this.repository = repository;
        this.mergerUserId = mergerUserId;
    }
}
