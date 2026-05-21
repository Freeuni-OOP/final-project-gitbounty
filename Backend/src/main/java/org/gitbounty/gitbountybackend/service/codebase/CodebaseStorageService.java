package org.gitbounty.gitbountybackend.service.codebase;

public interface CodebaseStorageService {

    void createRepository(String repositoryName);

    void deleteRepository(String repositoryName);
}

