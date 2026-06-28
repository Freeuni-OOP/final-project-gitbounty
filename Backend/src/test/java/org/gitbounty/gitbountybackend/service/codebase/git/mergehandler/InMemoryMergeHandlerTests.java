package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMergeHandlerTests {

    private InMemoryRepository repo;
    private TestRepository<InMemoryRepository> git;
    private InMemoryMergeHandler mergeHandler;

    @BeforeEach
    void setUp() throws IOException {
        // Create an isolated in-memory git repository context
        repo = new InMemoryRepository.Builder()
            .setRepositoryDescription(new DfsRepositoryDescription("test-repo"))
            .build();

        // Wrap it in JGit's TestRepository helper utility to easily mock commits/files
        git = new TestRepository<>(repo);

        // Instantiate the handler under test using the in-memory repository instance
        mergeHandler = new InMemoryMergeHandler(repo);
    }

    @Test
    void executeMerge_ShouldSucceed_WhenNoConflictsExist() throws Exception {
        // Arrange: Create a common ancestor commit
        RevCommit base = git.commit().message("Initial commit").add("file.txt", "base content").create();
        git.update("refs/heads/master", base);

        // Create target branch (develop) and modify file line 1
        RevCommit developCommit = git.branch("develop").commit()
            .parent(base)
            .message("Develop work")
            .add("file.txt", "base content\ndevelop change")
            .create();
        git.update("refs/heads/develop", developCommit);

        // Create source branch (feature) and modify a completely different file
        RevCommit featureCommit = git.branch("feature").commit()
            .parent(base)
            .message("Feature work")
            .add("feature.txt", "brand new feature content")
            .create();
        git.update("refs/heads/feature", featureCommit);

        // Act: Merge feature into develop
        assertDoesNotThrow(() -> mergeHandler.executeMerge("feature", "develop"));

        // Assert: Verify develop ref has moved to a new merge commit containing both changes
        ObjectId newDevelopTip = repo.resolve("refs/heads/develop");
        assertNotEquals(developCommit, newDevelopTip, "Develop branch pointer should have advanced.");

        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit mergeCommit = walk.parseCommit(newDevelopTip);
            assertEquals(2, mergeCommit.getParentCount(), "The resulting commit must be a merge commit (2 parents).");
            assertEquals(developCommit, mergeCommit.getParent(0), "First parent must be the target branch tip.");
            assertEquals(featureCommit, mergeCommit.getParent(1), "Second parent must be the source feature branch tip.");
        }
    }

    @Test
    void executeMerge_ShouldThrowMergeConflictException_WhenOverlappingChangesConflict() throws Exception {
        // Arrange: Create a common ancestor commit
        RevCommit base = git.commit().message("Initial commit").add("file.txt", "line 1").create();
        git.update("refs/heads/master", base);

        // Target branch edits line 1
        RevCommit developCommit = git.branch("develop").commit()
            .parent(base)
            .add("file.txt", "line 1 edited by develop")
            .create();
        git.update("refs/heads/develop", developCommit);

        // Source branch edits the exact same line 1 differently
        RevCommit featureCommit = git.branch("feature").commit()
            .parent(base)
            .add("file.txt", "line 1 edited by feature branch")
            .create();
        git.update("refs/heads/feature", featureCommit);

        // Act & Assert: Running merge should fail with our domain merge exception
        assertThrows(MergeConflictException.class, () ->
            mergeHandler.executeMerge("feature", "develop")
        );

        // Assert branch safety: Target branch pointer must remain unaffected
        assertEquals(developCommit, repo.resolve("refs/heads/develop"), "Develop tip must not change if conflict occurs.");
    }

    @Test
    void executeMerge_ShouldThrowIllegalArgumentException_WhenBranchDoesNotExist() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.executeMerge("non-existent-source", "master")
        );
    }

    @Test
    void revertMerge_ShouldSuccessfullyRollbackRefToPointerZero() throws Exception {
        // Arrange: Build standard merge history topology
        RevCommit base = git.commit().add("a", "1").create();
        RevCommit devTip = git.branch("develop").commit().parent(base).add("b", "2").create();
        RevCommit featureTip = git.branch("feature").commit().parent(base).add("c", "3").create();
        git.update("refs/heads/develop", devTip);
        git.update("refs/heads/feature", featureTip);

        // Merge feature into develop first to get a valid merge commit hash
        mergeHandler.executeMerge("feature", "develop");
        ObjectId mergeCommitId = repo.resolve("refs/heads/develop");

        // Act: Trigger rollback on our current repository branch pointer
        // Note: repo.getBranch() evaluates to the current active symbolic ref (defaults to master/HEAD)
        // Ensure HEAD points at our branch state for target testing environment consistency
        repo.updateRef(Constants.HEAD).link("refs/heads/develop");

        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop",mergeCommitId));

        // Assert: Ref pointer must match parent 0 (the state of develop right before merge)
        assertEquals(devTip, repo.resolve("refs/heads/develop"), "Branch should rollback exactly to devTip.");
    }
    @Test
    void revertMerge_ShouldDropSubsequentCommits_WhenNewCommitsExistAfterMerge() throws Exception {
        // Arrange: Create history baseline
        RevWalk walk = new RevWalk(repo);
        RevCommit base = git.commit().add("file.txt", "initial").create();
        RevCommit devTip = git.branch("develop").commit().parent(base).add("dev.txt", "1").create();
        RevCommit featureTip = git.branch("feature").commit().parent(base).add("feat.txt", "2").create();
        git.update("refs/heads/develop", devTip);
        git.update("refs/heads/feature", featureTip);

        // Merge feature into develop
        mergeHandler.executeMerge("feature", "develop");
        ObjectId mergeCommitId = repo.resolve("refs/heads/develop");

        // Simulate another developer adding a commit on top of the merge commit
        RevCommit postMergeCommit = git.branch("develop").commit()
            .parent(walk.parseCommit(mergeCommitId))
            .message("Accidental post-merge work")
            .add("extra.txt", "data")
            .create();
        git.update("refs/heads/develop", postMergeCommit);

        // Act: Fire the hard reset revert targeting the original merge commit
        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));

        // Assert: The branch pointer should skip right past postMergeCommit and land squarely on devTip
        ObjectId finalTip = repo.resolve("refs/heads/develop");
        assertEquals(devTip, finalTip, "The hard reset must roll back to the original dev tip, erasing subsequent commits.");
    }
    @Test
    void revertMerge_ShouldSucceedWithNoChange_WhenExecutedConsecutively() throws Exception {
        RevCommit base = git.commit().add("file.txt", "initial").create();
        RevCommit devTip = git.branch("develop").commit().parent(base).add("dev.txt", "1").create();
        RevCommit featureTip = git.branch("feature").commit().parent(base).add("feat.txt", "2").create();
        git.update("refs/heads/develop", devTip);
        git.update("refs/heads/feature", featureTip);

        mergeHandler.executeMerge("feature", "develop");
        ObjectId mergeCommitId = repo.resolve("refs/heads/develop");

        // Act: Revert once (moves pointer back to devTip)
        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));

        // Act Again: Revert a second time immediately (triggers NO_CHANGE state)
        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId),
            "Executing a duplicate revert should resolve gracefully as NO_CHANGE.");

        assertEquals(devTip, repo.resolve("refs/heads/develop"));
    }
    @Test
    void revertMerge_ShouldThrowIllegalArgumentException_WhenTargetBranchDoesNotExist() throws Exception {
        RevCommit base = git.commit().create();
        RevCommit p1 = git.commit().parent(base).create();
        RevCommit p2 = git.commit().parent(base).create();
        RevCommit dummyMerge = git.commit().parent(p1).parent(p2).create();

        // Act & Assert: Running against a fake branch name should throw an exception mapped from case NEW
        assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.revertMerge("non-existent-branch-name", dummyMerge)
        );
    }
    @Test
    void revertMerge_ShouldThrowIllegalArgumentException_WhenCommitIsNotAMergeCommit() throws Exception {
        // Arrange: Create a plain sequential commit (only 1 parent)
        RevCommit base = git.commit().create();
        RevCommit standardCommit = git.commit().parent(base).create();
        git.update("refs/heads/develop", standardCommit);

        // Act & Assert: Cannot execute a merge revert sequence against a non-merge tree configuration
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.revertMerge("develop", standardCommit)
        );

        assertTrue(exception.getMessage().contains("Commit is not a merge commit or has no parents."));
    }
}