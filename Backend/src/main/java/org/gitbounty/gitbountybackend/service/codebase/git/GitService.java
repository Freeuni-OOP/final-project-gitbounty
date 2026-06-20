package org.gitbounty.gitbountybackend.service.codebase.git;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.FileContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;
import org.gitbounty.gitbountybackend.util.codebase.RepositoryLockProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    public PathContents getPathContents(String repositoryName, String path, String branchName) {
        Path repoDir = Path.of(getRepoPath(repositoryName));

        try (Repository repository = new FileRepositoryBuilder().setGitDir(repoDir.toFile()).build();
             RevWalk revWalk = new RevWalk(repository)) {

            ObjectId branchId = repository.resolve(branchName);
            if (branchId == null) throw new IllegalArgumentException("Branch not found: " + branchName);

            RevTree tree = revWalk.parseCommit(branchId).getTree();

            // If path is root or empty, list the root tree
            if (path == null || path.isEmpty() || path.equals("/")) {
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(false);
                    return new DirectoryContents(path, listDirectory(treeWalk));
                }
            }

            // Use TreeWalk to find the specific path
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree)) {
                if (treeWalk == null) throw new IllegalArgumentException("Path not found: " + path);

                if (treeWalk.isSubtree()) {
                    // It's a directory: enter it and list its contents
                    treeWalk.enterSubtree();
                    return new DirectoryContents(path, listDirectory(treeWalk));
                } else {
                    // It's a file: read the blob
                    ObjectId blobId = treeWalk.getObjectId(0);
                    String content = new String(repository.open(blobId).getBytes(), StandardCharsets.UTF_8);
                    return new FileContents(treeWalk.getNameString(), content);
                }
            }
        } catch (IOException e) {
            throw new org.gitbounty.gitbountybackend.exception.GitAPIException("Error: " + e.getMessage());
        }
    }

    // Helper to extract directory entries
    private List<String> listDirectory(TreeWalk treeWalk) throws IOException {
        List<String> list = new ArrayList<>();
        while (treeWalk.next()) {
            list.add(treeWalk.getNameString());
        }
        return list;
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
        // 1. Sanitize the input to prevent directory traversal
        if (repositoryName.contains("..") || repositoryName.contains("/")) {
            throw new IllegalArgumentException("Invalid repository name format");
        }
        Path repoPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();

        // Verify it is strictly inside the root
        // Using toAbsolutePath().normalize() ensures we are comparing canonical paths
        if (!repoPath.startsWith(repositoriesRoot.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Access denied: Illegal path traversal attempt");
        }
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


    public void createRepository(String repositoryName) {
        // We lock here to prevent concurrent creation requests for the same repo name
        try {
            runLocked(repositoryName, () -> {
                Path repositoryPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();

                if (!repositoryPath.startsWith(repositoriesRoot)) {
                    throw new IllegalArgumentException("Invalid repository name: " + repositoryName);
                }

                if (Files.exists(repositoryPath)) {
                    throw new IllegalStateException("Repository directory already exists: " + repositoryName);
                }

                try {
                    Files.createDirectories(repositoriesRoot);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to create repository", e);
                }

                try (Git git = Git.init().setBare(true).setDirectory(repositoryPath.toFile()).call()) {
                    // Git.init successful
                } catch (GitAPIException e) {
                    cleanupRepositoryDirectory(repositoryPath);
                    throw new IllegalStateException("Unable to create repository", e);
                }
                return null; // Required for the functional interface
            });
        } catch (GitAPIException | IOException e) {
            throw new IllegalStateException("Git operation failed during creation", e);
        }
    }

    public void deleteRepository(String repositoryName) {
        try {
            runLocked(repositoryName, () -> {
                Path repositoryPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();
                if (!repositoryPath.startsWith(repositoriesRoot)) {
                    throw new IllegalArgumentException("Invalid repository name: " + repositoryName);
                }
                cleanupRepositoryDirectory(repositoryPath);
                return null; // Required for the functional interface
            });
        } catch (GitAPIException | IOException e) {
            throw new IllegalStateException("Git operation failed during deletion", e);
        }
    }

    private void cleanupRepositoryDirectory(Path repositoryPath) {
        if (!Files.exists(repositoryPath)) {
            return;
        }

        try (var paths = Files.walk(repositoryPath)) {
            paths.sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to clean up repository directory", e);
                    }
                });
        } catch (IOException e) {
            throw new org.gitbounty.gitbountybackend.exception.GitAPIException("Error cleaning up repository directory: " + repositoryPath);
        }
    }


    private boolean isValidBranchName(String name) {
        // Basic regex: allow only alphanumeric, hyphens, underscores, forward slashes
        return name != null && name.matches("^[a-zA-Z0-9/_\\-]+$");
    }

}
