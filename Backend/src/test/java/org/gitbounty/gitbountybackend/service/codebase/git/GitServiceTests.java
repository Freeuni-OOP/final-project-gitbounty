package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTests {

    private GitService gitService;
    private final String REPO_NAME = "test-repo";
    private File bareRepoDir;
    private PersonIdent mockUser;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        gitService = new GitService(tempDir, new LocalRepositoryLockProvider());
        bareRepoDir = tempDir.resolve(REPO_NAME + ".git").toFile();

        // Define standard operator details for the scope of the tests
        mockUser = new PersonIdent("Test Operator", "test-operator@gitbounty.org");

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
    void testSuccessfulMerge_ShouldStampIdentityAndFreshTimestamp() throws Exception {
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

        long expectedTimeSecs = Instant.now().getEpochSecond();

        // Test the service (passing identity parameters)
        MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "master", mockUser);

        assertTrue(result.getMergeStatus().isSuccessful());
        assertNotNull(result.getNewHead());

        // Thoroughly check that our handler stamped the credentials and real-time accurately
        try (Repository repository = new FileRepositoryBuilder().setGitDir(bareRepoDir).build();
             RevWalk walk = new RevWalk(repository)) {

            RevCommit mergeCommit = walk.parseCommit(result.getNewHead());
            assertEquals(mockUser.getName(), mergeCommit.getAuthorIdent().getName());
            assertEquals(mockUser.getEmailAddress(), mergeCommit.getCommitterIdent().getEmailAddress());

            long commitTimeSecs = mergeCommit.getCommitTime();
            assertTrue(Math.abs(expectedTimeSecs - commitTimeSecs) < 5, "Timestamp was stale or incorrect.");
        }
    }

    @Test
    void testMergeConflict() throws Exception {
        prepareBranch("feature", "file.txt", "feature change");
        prepareBranch("master", "file.txt", "master change");

        // Update signature parameters
        assertThrows(MergeConflictException.class, () ->
            gitService.mergeBranches(REPO_NAME, "feature", "master", mockUser)
        );
    }

    @Test
    void testSecuritySanitization() {
        assertThrows(IllegalArgumentException.class, () ->
            gitService.mergeBranches(REPO_NAME, "../../../etc/passwd", "master", mockUser)
        );
    }

    @Test
    void testConcurrencySafety() throws Exception {
        prepareBranch("f1", "f1.txt", "v1");
        prepareBranch("f2", "f2.txt", "v2");

        assertDoesNotThrow(() -> {
            gitService.mergeBranches(REPO_NAME, "f1", "master", mockUser);
            gitService.mergeBranches(REPO_NAME, "f2", "master", mockUser);
        });
    }

    @Test
    void testRollbackMergeSuccessful() throws Exception {
        String fileName = "rollback-test.txt";
        prepareBranch("feature", fileName, "content to be rolled back");

        MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "master", mockUser);
        assertTrue(result.getMergeStatus().isSuccessful());
        assertNotNull(result.getNewHead(), "Merge commit ID should be present");

        // Pass the identity sequence down into the revert pipeline
        gitService.revertMerge(REPO_NAME, "master", result.getNewHead(), mockUser);

        Path verificationClone = Files.createTempDirectory("verify-rollback");
        try (Git ignored = Git.cloneRepository()
            .setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(verificationClone.toFile())
            .call()) {

            File rolledBackFile = new File(verificationClone.toFile(), fileName);
            assertFalse(rolledBackFile.exists(), "File should not exist after rollback");
        }
    }

    @Test
    void testRollbackWithInvalidCommitId() {
        assertThrows(Exception.class, () ->
            gitService.revertMerge(REPO_NAME, "master", org.eclipse.jgit.lib.ObjectId.zeroId(), mockUser)
        );
    }

    private void prepareBranch(String branch, String file, String content) throws Exception {
        Path cloneDir = Files.createTempDirectory("conflict-clone");
        try (Git git = Git.cloneRepository().setURI(bareRepoDir.getAbsolutePath())
            .setDirectory(cloneDir.toFile()).call()) {

            boolean exists = git.branchList().call().stream()
                .anyMatch(ref -> ref.getName().equals("refs/heads/" + branch));

            git.checkout()
                .setCreateBranch(!exists)
                .setName(branch)
                .call();
            Path targetFile = cloneDir.resolve(file);
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

        gitService.createRepository(newRepoName);
        Path repoPath = bareRepoDir.toPath().getParent().resolve(newRepoName + ".git");
        assertTrue(Files.exists(repoPath), "Repository directory should exist");
        assertTrue(Files.exists(repoPath.resolve("config")), "Git config should exist");

        gitService.deleteRepository(newRepoName);
        assertFalse(Files.exists(repoPath), "Repository directory should be deleted");
    }

    @Test
    void testGetPathContents_RootDirectory() {
        PathContents result = gitService.getPathContents(REPO_NAME, "/", "master");

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

    @Test
    void testGetBranchDiff_ShouldReturnValidPatchString_WhenDifferencesExist() throws Exception {
        prepareBranch("master", "shared.txt", "line 1\nline 2\n");
        prepareBranch("feature", "shared.txt", "line 1\nline 2 edited\n");

        String diffOutput = gitService.getBranchDiff(REPO_NAME, "feature", "master");

        assertNotNull(diffOutput);
        assertTrue(diffOutput.contains("--- a/shared.txt"));
        assertTrue(diffOutput.contains("+++ b/shared.txt"));
        assertTrue(diffOutput.contains("-line 2"));
        assertTrue(diffOutput.contains("+line 2 edited"));
    }

    @Test
    void testGetBranchDiff_ShouldReturnEmptyString_WhenBranchesAreIdentical() throws Exception {
        prepareBranch("master", "sync.txt", "no changes");
        prepareBranch("feature-identical", "sync.txt", "no changes");

        String diffOutput = gitService.getBranchDiff(REPO_NAME, "feature-identical", "master");

        assertTrue(diffOutput.isEmpty(), "Diff output must be completely empty when histories match.");
    }

    @Test
    void testGetBranchDiff_ShouldThrowIllegalArgumentException_WhenBranchNamesAreMalicious() {
        assertThrows(IllegalArgumentException.class, () ->
            gitService.getBranchDiff(REPO_NAME, "feature", "../heads/master")
        );
    }
}