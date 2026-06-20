package org.gitbounty.gitbountybackend.service.codebase.storage;

public interface CodebaseStorageService {

    void createRepository(String repositoryName);

    void deleteRepository(String repositoryName);

    PathContents getPathContents(String repositoryName, String path, String branch);
}

