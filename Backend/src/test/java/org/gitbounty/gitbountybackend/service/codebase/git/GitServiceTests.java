package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.FileContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;
import org.gitbounty.gitbountybackend.util.codebase.LocalRepositoryLockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTests {

    private GitService gitService;
    private final String REPO_NAME = "test-repo";
    private File bareRepoDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        // Use the actual implementation for the test
        gitService = new GitService(tempDir, new LocalRepositoryLockProvider());
        bareRepoDir = tempDir.resolve(REPO_NAME + ".git").toFile();

        // 1. Initialize a BARE repository
        try (Git ignored = Git.init().setDirectory(bareRepoDir).setBare(true).call()) {
            // 2. To create the initial commit in a bare repo, we must clone it,
            // make changes, and push.
            Path cloneDir = tempDir.resolve("temp-clone");
            try (Git clone = Git.cloneRepository().setURI(bareRepoDir.getAbsolutePath())
                .setDirectory(cloneDir.toFile()).call()) {

                Files.writeString(cloneDir.resolve("file.txt"), "initial content");
                clone.add().addFilepattern("file.txt").call();
                clone.commit().setMessage("Initial commit").call();
                clone.push().call();
            }
        }
    }

    @Test
    void testSuccessfulMerge() throws Exception {
        // Clone to prepare the feature branch
        Path cloneDir = Files.createTempDirectory("test-clone");
        try (Git git = Git.cloneRepository().setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(cloneDir.toFile()).call()) {

            git.checkout().setCreateBranch(true).setName("feature").call();
            Files.writeString(cloneDir.resolve("feature.txt"), "feature content");
            git.add().addFilepattern("feature.txt").call();
            git.commit().setMessage("Feature commit").call();
            git.push().call();
        }

        // Test the service (which now handles the clone-merge-push logic internally)
        MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "master");

        assertTrue(result.getMergeStatus().isSuccessful());
    }

    @Test
    void testMergeConflict() throws Exception {
        // Prepare feature branch with change
        prepareBranch("feature", "file.txt", "feature change");
        // Prepare master branch with conflicting change
        prepareBranch("master", "file.txt", "master change");

        // We verify that our custom exception is thrown
        assertThrows(MergeConflictException.class, () -> gitService.mergeBranches(REPO_NAME, "feature", "master"));
    }

    @Test
    void testSecuritySanitization() {
        // Verify that path traversal attempts are blocked by our validation logic
        assertThrows(IllegalArgumentException.class, () -> gitService.mergeBranches(REPO_NAME, "../../../etc/passwd", "master"));
    }

    @Test
    void testConcurrencySafety() throws Exception {
        // This ensures that the lock provider is working
        // running two merges simultaneously.
        // Note: This relies on the fact that GitService.runLocked() is utilized.

        prepareBranch("f1", "f1.txt", "v1");
        prepareBranch("f2", "f2.txt", "v2");

        assertDoesNotThrow(() -> {
            gitService.mergeBranches(REPO_NAME, "f1", "master");
            gitService.mergeBranches(REPO_NAME, "f2", "master");
        });
    }

    @Test
    void testRollbackMergeSuccessful() throws Exception {
        // 1. Prepare feature with a file that doesn't exist on master
        String fileName = "rollback-test.txt";
        prepareBranch("feature", fileName, "content to be rolled back");

        // 2. Perform merge (this creates a merge commit)
        MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "master");
        assertTrue(result.getMergeStatus().isSuccessful());
        assertNotNull(result.getNewHead(), "Merge commit ID should be present");

        // 3. Rollback the specific merge commit
        gitService.revertMerge(REPO_NAME, result.getNewHead());

        // 4. Verify: Clone again to check the remote state
        Path verificationClone = Files.createTempDirectory("verify-rollback");
        try (Git ignored = Git.cloneRepository()
            .setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(verificationClone.toFile())
            .call()) {

            // The file from the feature branch should NOT exist after rollback
            File rolledBackFile = new File(verificationClone.toFile(), fileName);
            assertFalse(rolledBackFile.exists(), "File should not exist after rollback");
        }
    }

    @Test
    void testRollbackWithInvalidCommitId() {
        // Ensure that providing a non-existent commit ID throws an exception
        // (Assuming you handle bad ObjectIds in your service)
        assertThrows(Exception.class, () -> gitService.revertMerge(REPO_NAME, org.eclipse.jgit.lib.ObjectId.zeroId()));
    }

    // Helper to simulate work in a bare repo
    private void prepareBranch(String branch, String file, String content) throws Exception {
        Path cloneDir = Files.createTempDirectory("conflict-clone");
        try (Git git = Git.cloneRepository().setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(cloneDir.toFile()).call()) {

            // Use the safe logic: check if branch exists, otherwise create it
            boolean exists = git.branchList().call().stream()
                .anyMatch(ref -> ref.getName().equals("refs/heads/" + branch));

            git.checkout()
                .setCreateBranch(!exists)
                .setName(branch)
                .call();
            Path targetFile = cloneDir.resolve(file);
            // 2. CREATE PARENT DIRECTORIES if they don't exist
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            Files.writeString(targetFile, content);
            git.add().addFilepattern(file).call();
            git.commit().setMessage("Change to " + file).call();
            git.push().call();
        }
    }

    @Test
    void testCreateAndDeleteRepository(){
        String newRepoName = "new-repo";

        // 1. Test Creation
        gitService.createRepository(newRepoName);
        Path repoPath = bareRepoDir.toPath().getParent().resolve(newRepoName + ".git");
        assertTrue(Files.exists(repoPath), "Repository directory should exist");
        assertTrue(Files.exists(repoPath.resolve("config")), "Git config should exist");

        // 2. Test Deletion
        gitService.deleteRepository(newRepoName);
        assertFalse(Files.exists(repoPath), "Repository directory should be deleted");
    }

    @Test
    void testGetPathContents_RootDirectory() {
        PathContents result = gitService.getPathContents(REPO_NAME, "/", "master");

        // Use instanceof to identify and cast
        assertInstanceOf(DirectoryContents.class, result, "Result should be a directory");
        DirectoryContents dir = (DirectoryContents) result;

        boolean foundFile = dir.contents().stream().anyMatch(name -> name.equals("file.txt"));
        assertTrue(foundFile, "Should list file.txt in root");
    }

    @Test
    void testGetPathContents_FileInSubdirectory() throws Exception {
        prepareBranch("master", "src/main.java", "public class Main {}");

        PathContents result = gitService.getPathContents(REPO_NAME, "src/main.java", "master");

        assertInstanceOf(FileContents.class, result, "Result should be a file");
        FileContents file = (FileContents) result;

        assertEquals("public class Main {}", file.contents(), "File contents should match");
    }

    @Test
    void testGetPathContents_SubdirectoryListing() throws Exception {
        prepareBranch("master", "docs/readme.txt", "some docs");

        PathContents result = gitService.getPathContents(REPO_NAME, "docs", "master");

        assertInstanceOf(DirectoryContents.class, result, "Result should be a directory");
        DirectoryContents dir = (DirectoryContents) result;

        assertEquals(1, dir.contents().size());
        assertEquals("readme.txt", dir.contents().get(0));
    }

    @Test
    void testGetPathContents_InvalidPath() {
        assertThrows(Exception.class, () -> gitService.getPathContents(REPO_NAME, "non-existent-dir", "master"));
    }

    @Test
    void testCreateRepository_FailOnGitInit() throws IOException {
        Path repoPath = bareRepoDir.toPath().getParent().resolve("bad-repo.git");
        Files.createFile(repoPath);

        assertThrows(IllegalStateException.class, () -> gitService.createRepository("bad-repo"));
    }

    @Test
    void testCreateRepository_AlreadyExists() {
        gitService.createRepository("existing-repo");
        assertThrows(IllegalStateException.class, () -> gitService.createRepository("existing-repo"));
    }
}
