package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.gitbounty.gitbountybackend.service.codebase.git.mergehandler.InMemoryMergeHandler;
import org.gitbounty.gitbountybackend.service.codebase.git.mergehandler.MergeHandler;
import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.FileContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;
import org.gitbounty.gitbountybackend.util.codebase.RepositoryLockProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
public class GitService {

    private final Path repositoriesRoot;
    private final RepositoryLockProvider repositoryLockProvider;

    public GitService(Path repositoriesRoot, RepositoryLockProvider repositoryLockProvider) {
        this.repositoriesRoot = repositoriesRoot;
        this.repositoryLockProvider = repositoryLockProvider;
    }

    public <T> T runLocked(String repositoryName, SupplierWithException<T> task) throws GitAPIException, IOException {
        Lock lock = repositoryLockProvider.getLock(repositoryName);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Computes the raw git diff text comparing a source branch against a target branch.
     * Equivalent to running: git diff targetBranch sourceBranch
     */
    public String getBranchDiff(String repositoryName, String sourceBranch, String targetBranch) throws IOException {
        if (!isValidBranchName(sourceBranch) || !isValidBranchName(targetBranch)) {
            throw new IllegalArgumentException("Invalid branch names provided.");
        }

        File repoDir = new File(getRepoPath(repositoryName));

        try (Repository repository = new FileRepositoryBuilder().setGitDir(repoDir).build();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(out);
             RevWalk walk = new RevWalk(repository)) {

            formatter.setRepository(repository);

            ObjectId sourceId = repository.resolve(sourceBranch);
            ObjectId targetId = repository.resolve(targetBranch);

            if (sourceId == null || targetId == null) {
                throw new IllegalArgumentException("One or both branches could not be found.");
            }

            AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, walk, targetId);
            AbstractTreeIterator newTreeParser = prepareTreeParser(repository, walk, sourceId);

            List<DiffEntry> diffs = formatter.scan(oldTreeParser, newTreeParser);
            for (DiffEntry entry : diffs) {
                formatter.format(entry);
            }

            return out.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Executes an isolated in-memory merge utilizing the detached MergeHandler blueprint.
     */
    public MergeResult mergeBranches(String repositoryName, String sourceBranch, String targetBranch, PersonIdent mergeUserId) throws GitAPIException, IOException {
        if (!isValidBranchName(sourceBranch) || !isValidBranchName(targetBranch)) {
            throw new IllegalArgumentException("Invalid branch names.");
        }

        return runLocked(repositoryName, () -> {
            File repoDir = new File(getRepoPath(repositoryName));
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repoDir).build()) {
                MergeHandler handler = new InMemoryMergeHandler(repository, mergeUserId);
                return handler.executeMerge(sourceBranch, targetBranch);
            }
        });
    }

    /**
     * Reverts a merge commit reference pointer back to its mainline parent.
     */
    public void revertMerge(String repositoryName, String branchName, ObjectId mergeCommitId, PersonIdent userId) throws GitAPIException, IOException {
        runLocked(repositoryName, () -> {
            File repoDir = new File(getRepoPath(repositoryName));
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repoDir).build()) {
                MergeHandler handler = new InMemoryMergeHandler(repository, userId);
                handler.revertMerge(branchName, mergeCommitId);
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
                Files.createDirectories(repositoriesRoot);
                Git.init().setBare(true).setDirectory(repositoryPath.toFile()).call().close();
                return null;
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

    private AbstractTreeIterator prepareTreeParser(Repository repository, RevWalk walk, ObjectId commitId) throws IOException {
        RevCommit commit = walk.parseCommit(commitId);
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree.getId());
        }
        return treeParser;
    }

    private List<String> listDirectory(TreeWalk treeWalk) throws IOException {
        List<String> list = new ArrayList<>();
        while (treeWalk.next()) {
            list.add(treeWalk.getNameString());
        }
        return list;
    }

    private String getRepoPath(String repositoryName) {
        if (repositoryName.contains("..") || repositoryName.contains("/")) {
            throw new IllegalArgumentException("Invalid repository name format");
        }
        Path repoPath = repositoriesRoot.resolve(repositoryName + ".git").normalize();
        if (!repoPath.startsWith(repositoriesRoot.toAbsolutePath().normalize()) || !Files.exists(repoPath)) {
            throw new IllegalArgumentException("Invalid or non-existent repository: " + repositoryName);
        }
        return repoPath.toAbsolutePath().toString();
    }

    private void cleanupRepositoryDirectory(Path repositoryPath) {
        if (!Files.exists(repositoryPath)) return;
        try (var paths = Files.walk(repositoryPath)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IOException e) {
            throw new org.gitbounty.gitbountybackend.exception.GitAPIException("Cleanup failure: " + repositoryPath);
        }
    }

    private boolean isValidBranchName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9/_\\-]+$");
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws IOException, GitAPIException;
    }
}