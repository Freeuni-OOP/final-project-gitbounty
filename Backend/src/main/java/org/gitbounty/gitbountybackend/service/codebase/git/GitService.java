package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitService {

    private final Path repositoriesRoot;

    // Inject the root path created in your GitServletConfiguration
    public GitService(Path repositoriesRoot) {
        this.repositoriesRoot = repositoriesRoot;
    }

    /**
     * Gets the absolute path for a specific repository directory.
     * @param repositoryName The name of the repo (e.g., "my-project.git")
     * @return String path
     */
    private String getRepoPath(String repositoryName) {
        Path repoPath = repositoriesRoot.resolve(repositoryName).normalize();

        if (!repoPath.startsWith(repositoriesRoot) || !Files.exists(repoPath)) {
            throw new IllegalArgumentException("Invalid or non-existent repository: " + repositoryName);
        }

        return repoPath.toAbsolutePath().toString();
    }

    public MergeResult mergeBranches(String repositoryName, String sourceBranch, String targetBranch)
        throws IOException, GitAPIException {
        File repoDir = new File(getRepoPath(repositoryName));

        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(new File(repoDir, ".git"))
            .readEnvironment()
            .findGitDir()
            .build();
             Git git = new Git(repository)) {

            // 1. Checkout the target branch
            git.checkout().setName(targetBranch).call();

            // 2. Perform the merge
            // We refer to the source branch using the ref name
            return git.merge()
                .include(repository.findRef(sourceBranch))
                .setCommit(true)
                .setMessage("Merge " + sourceBranch + " into " + targetBranch)
                .call();
        }
    }
}
