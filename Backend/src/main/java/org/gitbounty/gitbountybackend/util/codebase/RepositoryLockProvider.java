package org.gitbounty.gitbountybackend.util.codebase;

import java.util.concurrent.locks.Lock;

public interface RepositoryLockProvider {
    // Returns a lock object that can be locked/unlocked
    Lock getLock(String repositoryName);
}
