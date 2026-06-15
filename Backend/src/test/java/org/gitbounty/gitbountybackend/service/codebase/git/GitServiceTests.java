package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTests {

    private GitService gitService;
    private final String REPO_NAME = "test-repo";
    private File bareRepoDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        gitService = new GitService(tempDir);
        bareRepoDir = tempDir.resolve(REPO_NAME).toFile();

        // 1. Initialize a BARE repository
        try (Git git = Git.init().setDirectory(bareRepoDir).setBare(true).call()) {
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

        // Attempt merge
        MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "master");

        assertEquals(MergeResult.MergeStatus.CONFLICTING, result.getMergeStatus());
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

            Files.writeString(cloneDir.resolve(file), content);
            git.add().addFilepattern(file).call();
            git.commit().setMessage("Change to " + file).call();
            git.push().call();
        }
    }

    @Test
    void testMaliciousMergeTargetInjection(){
        // Assume "main" is a protected branch and a user tries to inject a merge
        // into a branch they shouldn't be able to touch or that doesn't exist.

        // Attempt to merge into a non-existent or forbidden branch
        // The service should ideally throw an exception rather than creating or corrupting state
        assertThrows(Exception.class, () -> {
            gitService.mergeBranches(REPO_NAME, "feature", "malicious-branch-name");
        });
    }

    @Test
    void testMergeAttemptWithInvalidBranchNames(){
        // A user tries to use path traversal-like branch names
        // Git branch names can technically contain many characters
        // sanitizing this is a security necessity.
        String maliciousBranch = "../../../etc/passwd";

        assertThrows(Exception.class, () -> {
            gitService.mergeBranches(REPO_NAME, maliciousBranch, "master");
        });
    }
}