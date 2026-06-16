package org.gitbounty.gitbountybackend.service.codebase.git;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.gitbounty.gitbountybackend.util.codebase.RepositoryLockProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;

@Service
public class GitService {

    private final Path repositoriesRoot;
    private final String MERGE_WORKSPACE_DIRECTORY = System.getProperty("java.io.tmpdir");
    private final RepositoryLockProvider repositoryLockProvider;

    // Inject the root path created in your GitServletConfiguration
    public GitService(Path repositoriesRoot, RepositoryLockProvider repositoryLockProvider) {
        this.repositoriesRoot = repositoriesRoot;
        this.repositoryLockProvider = repositoryLockProvider;
    }

    /**
     * Executes a task while holding a lock on the specific repository.
     * This ensures atomicity across multiple Git operations.
     */
    public <T> T runLocked(String repositoryName, SupplierWithException<T> task) throws GitAPIException, IOException {
        Lock lock = repositoryLockProvider.getLock(repositoryName);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    public void revertMerge(String repositoryName, ObjectId mergeCommitId) throws GitAPIException, IOException {
        runLocked(repositoryName, () -> {
            File repoDir = new File(getRepoPath(repositoryName));
            Path tempDir = Files.createTempDirectory(Paths.get(MERGE_WORKSPACE_DIRECTORY), "rollback-");

            try (Git git = Git.cloneRepository()
                .setURI(repoDir.getAbsolutePath())
                .setDirectory(tempDir.toFile())
                .call()) {

                // 1. Locate the parent (the state before merge)
                try (RevWalk walk = new RevWalk(git.getRepository())) {
                    RevCommit mergeCommit = walk.parseCommit(mergeCommitId);
                    // Parent 0 is the mainline (e.g., master/main)
                    RevCommit mainlineParent = mergeCommit.getParent(0);

                    // 2. Reset the branch pointer to the state BEFORE the merge
                    git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(mainlineParent.getName())
                        .call();

                    // 3. Force push the reset state to overwrite the remote history
                    // This makes the remote match the state before the merge
                    git.push()
                        .setRemote("origin")
                        .setForce(true) // Required to overwrite the history
                        .call();
                }
            } finally {
                FileUtils.deleteDirectory(tempDir.toFile());
            }
            return null;
        });
    }

    // Functional interface to allow throwing checked exceptions
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws IOException, GitAPIException;
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

    private MergeResult performMerge(String repositoryName, String sourceBranch, String targetBranch)
        throws IOException, GitAPIException, MergeConflictException {

        if(!isValidBranchName(sourceBranch) || !isValidBranchName(targetBranch)) {
            throw new IllegalArgumentException("Invalid branch name. Allowed characters: alphanumeric, hyphens, underscores, forward slashes.");
        }

        File bareRepo = new File(getRepoPath(repositoryName));
        Path tempDir = Files.createTempDirectory(Paths.get(MERGE_WORKSPACE_DIRECTORY), "merge-" + repositoryName + "-");

        try (Git git = Git.cloneRepository()
            .setURI(bareRepo.getAbsolutePath())
            .setDirectory(tempDir.toFile())
            .call()) {

            git.checkout().setName(targetBranch).call();

            MergeResult result = git.merge()
                .include(git.getRepository().findRef("refs/remotes/origin/" + sourceBranch))
                .setMessage("Merge " + sourceBranch + " into " + targetBranch)
                .call();

            if (!result.getMergeStatus().isSuccessful()) {
                throw new MergeConflictException("Merge conflict detected: " + result.getMergeStatus());
            }

            git.push().setRemote("origin").call();

            return result;

        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
    public MergeResult mergeBranches(String repositoryName, String sourceBranch, String targetBranch) throws GitAPIException, IOException {
        return runLocked(repositoryName, () -> performMerge(repositoryName, sourceBranch, targetBranch));
    }


    private boolean isValidBranchName(String name) {
        // Basic regex: allow only alphanumeric, hyphens, underscores, forward slashes
        return name != null && name.matches("^[a-zA-Z0-9/_\\-]+$");
    }
}
