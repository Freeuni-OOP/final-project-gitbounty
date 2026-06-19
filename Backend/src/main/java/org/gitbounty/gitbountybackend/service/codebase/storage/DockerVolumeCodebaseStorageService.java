package org.gitbounty.gitbountybackend.service.codebase.storage;


import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DockerVolumeCodebaseStorageService implements CodebaseStorageService {

    private final GitService gitService;

    @Autowired
    public DockerVolumeCodebaseStorageService(GitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public void createRepository(String repositoryName) {
        gitService.createRepository(repositoryName);
    }

    @Override
    public void deleteRepository(String repositoryName) {
        gitService.deleteRepository(repositoryName);
    }

    @Override
    public PathContents getPathContents(String repositoryName, String path, String branch) {
        return gitService.getPathContents(repositoryName, path, branch);
    }

}

