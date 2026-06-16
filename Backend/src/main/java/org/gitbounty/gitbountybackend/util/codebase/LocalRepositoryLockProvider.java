package org.gitbounty.gitbountybackend.util.codebase;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LocalRepositoryLockProvider implements RepositoryLockProvider {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Lock getLock(String repositoryName) {
        return locks.computeIfAbsent(repositoryName, k -> new ReentrantLock());
    }
}
