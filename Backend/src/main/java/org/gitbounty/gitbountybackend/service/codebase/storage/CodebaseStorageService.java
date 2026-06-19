package org.gitbounty.gitbountybackend.service.codebase.storage;

import java.util.List;

public interface CodebaseStorageService {

    void createRepository(String repositoryName);

    void deleteRepository(String repositoryName);

    List<CodebaseEntry> listDirectoryContents(String repositoryName, String path, String branchName);
}

