package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMergeHandlerTests {

    private InMemoryRepository repo;
    private TestRepository<InMemoryRepository> git;
    private InMemoryMergeHandler mergeHandler;
    private PersonIdent mergerUser;

    @BeforeEach
    void setUp() throws IOException {
        repo = new InMemoryRepository.Builder()
            .setRepositoryDescription(new DfsRepositoryDescription("test-repo"))
            .build();

        git = new TestRepository<>(repo);

        // Standard test user configuration
        mergerUser = new PersonIdent("Bounty Operator", "operator@gitbounty.org");
        mergeHandler = new InMemoryMergeHandler(repo, mergerUser);
    }

    @Test
    void executeMerge_ShouldSucceed_AndApplyCorrectCommitterAndFreshTimestamp() throws Exception {
        // Arrange: Create a common ancestor commit
        RevCommit base = git.commit().message("Initial commit").add("file.txt", "base content").create();
        git.update("refs/heads/master", base);

        RevCommit developCommit = git.branch("develop").commit()
            .parent(base)
            .message("Develop work")
            .add("file.txt", "base content\ndevelop change")
            .create();
        git.update("refs/heads/develop", developCommit);

        RevCommit featureCommit = git.branch("feature").commit()
            .parent(base)
            .message("Feature work")
            .add("feature.txt", "brand new feature content")
            .create();
        git.update("refs/heads/feature", featureCommit);

        // Capture the wall-clock time right before execution (Git stores seconds, so truncate to seconds)
        long expectedTimeSecs = Instant.now().getEpochSecond();

        // Act: Execute the merge
        assertDoesNotThrow(() -> mergeHandler.executeMerge("feature", "develop"));

        // Assert
        ObjectId newDevelopTip = repo.resolve("refs/heads/develop");
        assertNotEquals(developCommit, newDevelopTip);

        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit mergeCommit = walk.parseCommit(newDevelopTip);

            // 1. Verify User Identity Structure
            assertEquals(mergerUser.getName(), mergeCommit.getAuthorIdent().getName());
            assertEquals(mergerUser.getEmailAddress(), mergeCommit.getCommitterIdent().getEmailAddress());

            // 2. Verify Dynamic Timing Window Safety
            long commitTimeSecs = mergeCommit.getCommitTime(); // Returns Unix epoch seconds

            // Allow a tiny 5-second buffer for execution latency
            long timeDelta = Math.abs(expectedTimeSecs - commitTimeSecs);
            assertTrue(timeDelta < 5,
                "Commit timestamp was out of bounds! Expected close to " + expectedTimeSecs + " but got " + commitTimeSecs);
        }
    }

    @Test
    void executeMerge_ShouldThrowMergeConflictException_WhenOverlappingChangesConflict() throws Exception {
        RevCommit base = git.commit().message("Initial commit").add("file.txt", "line 1").create();
        git.update("refs/heads/master", base);

        RevCommit developCommit = git.branch("develop").commit().parent(base).add("file.txt", "line 1 edited by develop").create();
        git.update("refs/heads/develop", developCommit);

        RevCommit featureCommit = git.branch("feature").commit().parent(base).add("file.txt", "line 1 edited by feature branch").create();
        git.update("refs/heads/feature", featureCommit);

        assertThrows(MergeConflictException.class, () ->
            mergeHandler.executeMerge("feature", "develop")
        );
    }

    @Test
    void executeMerge_ShouldThrowIllegalArgumentException_WhenBranchDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.executeMerge("non-existent-source", "master")
        );
    }

    @Test
    void revertMerge_ShouldSuccessfullyRollbackRefToPointerZero() throws Exception {
        RevCommit base = git.commit().add("a", "1").create();
        RevCommit devTip = git.branch("develop").commit().parent(base).add("b", "2").create();
        RevCommit featureTip = git.branch("feature").commit().parent(base).add("c", "3").create();
        git.update("refs/heads/develop", devTip);
        git.update("refs/heads/feature", featureTip);

        mergeHandler.executeMerge("feature", "develop");
        ObjectId mergeCommitId = repo.resolve("refs/heads/develop");

        repo.updateRef(Constants.HEAD).link("refs/heads/develop");

        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));
        assertEquals(devTip, repo.resolve("refs/heads/develop"));
    }

    @Test
    void revertMerge_ShouldDropSubsequentCommits_WhenNewCommitsExistAfterMerge() throws Exception {
        RevWalk walk = new RevWalk(repo);
        RevCommit base = git.commit().add("file.txt", "initial").create();
        RevCommit devTip = git.branch("develop").commit().parent(base).add("dev.txt", "1").create();
        RevCommit featureTip = git.branch("feature").commit().parent(base).add("feat.txt", "2").create();
        git.update("refs/heads/develop", devTip);
        git.update("refs/heads/feature", featureTip);

        mergeHandler.executeMerge("feature", "develop");
        ObjectId mergeCommitId = repo.resolve("refs/heads/develop");

        RevCommit postMergeCommit = git.branch("develop").commit()
            .parent(walk.parseCommit(mergeCommitId))
            .message("Accidental post-merge work")
            .add("extra.txt", "data")
            .create();
        git.update("refs/heads/develop", postMergeCommit);

        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));

        ObjectId finalTip = repo.resolve("refs/heads/develop");
        assertEquals(devTip, finalTip);
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

        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));
        assertDoesNotThrow(() -> mergeHandler.revertMerge("develop", mergeCommitId));

        assertEquals(devTip, repo.resolve("refs/heads/develop"));
    }

    @Test
    void revertMerge_ShouldThrowIllegalArgumentException_WhenTargetBranchDoesNotExist() throws Exception {
        RevCommit base = git.commit().create();
        RevCommit p1 = git.commit().parent(base).create();
        RevCommit p2 = git.commit().parent(base).create();
        RevCommit dummyMerge = git.commit().parent(p1).parent(p2).create();

        assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.revertMerge("non-existent-branch-name", dummyMerge)
        );
    }

    @Test
    void revertMerge_ShouldThrowIllegalArgumentException_WhenCommitIsNotAMergeCommit() throws Exception {
        RevCommit base = git.commit().create();
        RevCommit standardCommit = git.commit().parent(base).create();
        git.update("refs/heads/develop", standardCommit);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            mergeHandler.revertMerge("develop", standardCommit)
        );

        assertTrue(exception.getMessage().contains("Commit is not a merge commit or has no parents."));
    }
}