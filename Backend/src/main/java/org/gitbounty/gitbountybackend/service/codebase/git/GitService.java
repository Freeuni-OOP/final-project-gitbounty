package org.gitbounty.gitbountybackend.service.codebase.git;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.gitbounty.gitbountybackend.exception.BranchNotFoundException;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseEntry;
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

    public List<CodebaseEntry> listDirectoryContents(String repositoryName, String path, String branchName){
        List<CodebaseEntry> entries = new ArrayList<>();

        // Construct path to the .git directory
        Path repoDir = repositoriesRoot.resolve(repositoryName + ".git");

        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(repoDir.toFile())
            .build();
             RevWalk revWalk = new RevWalk(repository)) {

            // Resolve the specific branch
            ObjectId branchId = repository.resolve(branchName);
            if (branchId == null) {
                throw new BranchNotFoundException("Branch not found: " + branchName);
            }

            //Get the root tree of that branch
            RevTree tree = revWalk.parseCommit(branchId).getTree();

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(false); // dont list all the subdirectory contents

                if (path != null && !path.isEmpty() && !path.equals("/")) {
                    String cleanPath = path.startsWith("/") ? path.substring(1) : path;
                    treeWalk.setFilter(PathFilter.create(cleanPath));

                    if (!treeWalk.next()) {
                        throw new IllegalArgumentException("Path not found in repository: " + path);
                    }
                    if (treeWalk.isSubtree()) {
                        treeWalk.enterSubtree();
                    }
                }

                // Iterate through contents
                while (treeWalk.next()) {
                    entries.add(new CodebaseEntry(
                        treeWalk.getNameString(),
                        treeWalk.isSubtree() // if the current pointer is a file or directory
                    ));
                }
            }
        }
        catch (IOException e) {
            throw new org.gitbounty.gitbountybackend.exception.GitAPIException("Error accessing repository: " + repositoryName);
        }
        return entries;
    }

    public String getFileContents(String repositoryName, String path, String branchName) throws IOException {
        Path repoDir = repositoriesRoot.resolve(repositoryName + ".git");

        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(repoDir.toFile())
            .build();
             RevWalk revWalk = new RevWalk(repository)) {

            // Resolve the branch to a commit
            ObjectId branchId = repository.resolve(branchName);
            if (branchId == null) {
                throw new IllegalArgumentException("Branch not found: " + branchName);
            }

            // Get the tree from the commit
            RevTree tree = revWalk.parseCommit(branchId).getTree();

            // Use TreeWalk to find the file
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree)) {
                if (treeWalk == null) {
                    throw new IllegalArgumentException("File not found in repository: " + path);
                }

                // Load the blob object
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(blobId);

                // Convert bytes to string (assuming UTF-8)
                return new String(loader.getBytes(), StandardCharsets.UTF_8);
            }
        }
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
        Path repoPath = repositoriesRoot.resolve(repositoryName + ".git");

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
            // Touch repository to avoid an empty try block while still relying on JGit resource cleanup.
            git.getRepository();
        } catch (GitAPIException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw new IllegalStateException("Unable to create repository", e);
        } catch (RuntimeException e) {
            cleanupRepositoryDirectory(repositoryPath);
            throw e;
        }
    }

    public void deleteRepository(String repositoryName) {
        Path repositoryPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();
        if (!repositoryPath.startsWith(repositoriesRoot)) {
            throw new IllegalArgumentException("Invalid repository name: " + repositoryName);
        }
        cleanupRepositoryDirectory(repositoryPath);
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
