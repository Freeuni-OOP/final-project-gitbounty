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

class GitServiceTest {

    private GitService gitService;
    private final String REPO_NAME = "test-repo";
    private File repoDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        // tempDir acts as the repositoriesRoot
        gitService = new GitService(tempDir);

        // Create the directory structure: tempDir/test-repo/.git
        repoDir = tempDir.resolve(REPO_NAME).toFile();
        assertTrue(repoDir.mkdir());

        // Initialize the repository
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            Files.writeString(new File(repoDir, "file.txt").toPath(), "initial content");
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("Initial commit").call();
            git.branchCreate().setName("main").call();
        }
    }

    @Test
    void testSuccessfulMerge() throws Exception {
        try (Git git = Git.open(repoDir)) {
            // Create feature branch
            git.checkout().setCreateBranch(true).setName("feature").call();
            Files.writeString(new File(repoDir, "feature.txt").toPath(), "feature content");
            git.add().addFilepattern("feature.txt").call();
            git.commit().setMessage("Feature commit").call();

            // Merge feature into main using repository name
            MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "main");

            assertTrue(result.getMergeStatus().isSuccessful());
        }
    }

    @Test
    void testMergeConflict() throws Exception {
        try (Git git = Git.open(repoDir)) {
            // 1. Create and edit file in feature branch
            git.checkout().setCreateBranch(true).setName("feature").call();
            Files.writeString(new File(repoDir, "file.txt").toPath(), "feature change");
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("Feature change").call();

            // 2. Edit same file in main branch
            git.checkout().setName("main").call();
            Files.writeString(new File(repoDir, "file.txt").toPath(), "main change");
            git.add().addFilepattern("file.txt").call();
            git.commit().setMessage("Main change").call();

            // 3. Attempt merge using repository name
            MergeResult result = gitService.mergeBranches(REPO_NAME, "feature", "main");

            assertEquals(MergeResult.MergeStatus.CONFLICTING, result.getMergeStatus());
            assertNotNull(result.getConflicts());
        }
    }
}