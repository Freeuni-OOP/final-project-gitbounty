package org.gitbounty.gitbountybackend.service.codebase.storage;

public interface CodebaseStorageService {

    void createRepository(String repositoryName);

    void deleteRepository(String repositoryName);
}

