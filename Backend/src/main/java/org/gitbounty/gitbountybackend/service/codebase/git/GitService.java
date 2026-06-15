package org.gitbounty.gitbountybackend.service.codebase.git;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GitService {

    private final Path repositoriesRoot;
    private final String MERGE_WORKSPACE_DIRECTORY = System.getProperty("java.io.tmpdir");

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

        // 1. Sanitize Inputs (Crucial for security)
        if (!isValidBranchName(sourceBranch) || !isValidBranchName(targetBranch)) {
            throw new IllegalArgumentException("Invalid branch name format.");
        }

        File bareRepoDir = new File(getRepoPath(repositoryName));
        Path baseTempDir = Paths.get(MERGE_WORKSPACE_DIRECTORY);
        Files.createDirectories(baseTempDir);

        // Use a unique name for the temporary working directory
        Path tempDirPath = Files.createTempDirectory(baseTempDir, "merge-" + repositoryName + "-");
        File tempDir = tempDirPath.toFile();

        try {
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                // 1. Clone inside the try-block to ensure fresh state per attempt
                try (Git git = Git.cloneRepository()
                    .setURI(bareRepoDir.getAbsolutePath())
                    .setDirectory(tempDir)
                    .call()) {

                    git.checkout().setName(targetBranch).call();

                    // 2. Perform merge
                    MergeResult result = git.merge()
                        .include(git.getRepository().findRef("refs/remotes/origin/" + sourceBranch))
                        .setMessage("Merge " + sourceBranch + " into " + targetBranch)
                        .call();

                    if (!result.getMergeStatus().isSuccessful()) {
                        return result; // Exit if merge failed (conflicts)
                    }

                    // 3. Attempt push with retry logic
                    try {
                        git.push().setRemote("origin").call();
                        return result; // Success!
                    } catch (TransportException e) {
                        if (attempt == maxRetries) throw e;
                        // Potential race condition (e.g., branch updated since clone), loop and retry
                        git.pull().setRemote("origin").call();
                    }
                }
            }
            throw new GitAPIException("Failed to push changes after " + maxRetries + " attempts.") {};
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private boolean isValidBranchName(String name) {
        // Basic regex: allow only alphanumeric, hyphens, underscores, forward slashes
        return name != null && name.matches("^[a-zA-Z0-9/_\\-]+$");
    }
}
