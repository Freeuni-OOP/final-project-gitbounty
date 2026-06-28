package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gitbounty.gitbountybackend.exception.GitAPIException;
import org.gitbounty.gitbountybackend.exception.MergeConflictException;

import java.io.IOException;

public class InMemoryMergeHandler extends AbstractMergeHandler {

    public InMemoryMergeHandler(Repository repository) {
        super(repository);
    }

    @Override
    public MergeResult executeMerge(String sourceBranch, String targetBranch) {
        Repository repo = this.repository;

        try (RevWalk walk = new RevWalk(repo)) {
            ObjectId sourceId = repo.resolve(sourceBranch);
            ObjectId targetId = repo.resolve(targetBranch);

            if (sourceId == null || targetId == null) {
                throw new IllegalArgumentException("Target or Source branch was missing.");
            }

            RevCommit sourceCommit = walk.parseCommit(sourceId);
            RevCommit targetCommit = walk.parseCommit(targetId);

            // Attempt an in-memory 3-way merge execution
            ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repo, true);
            boolean cleanMerge = merger.merge(targetCommit, sourceCommit);

            if (!cleanMerge) {
                throw new MergeConflictException("Merge conflict discovered via in-memory analysis.");
            }

            String commitMessage = "Merge branch '" + sourceBranch + "' into " + targetBranch;
            ObjectId mergeCommit = createMergeCommit(repo, merger.getResultTreeId(), sourceCommit, targetCommit, commitMessage);

            updateRef(repo, mergeCommit, targetId, targetBranch);

            // Clean merge achieved! Return a successful result holding the new HEAD commit ID
            return new MergeResult(
                mergeCommit,
                targetId,
                new ObjectId[] { targetId, sourceId },
                MergeResult.MergeStatus.MERGED,
                MergeStrategy.RECURSIVE,
                null,
                null
            );

        } catch (IOException e) {
            throw new GitAPIException("In-memory merge execution failed" + e);
        }
    }

    @Override
    public void revertMerge(String branchName, ObjectId mergeCommitId) {
        Repository repo = this.repository;

        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit mergeCommit = walk.parseCommit(mergeCommitId);

            if (mergeCommit.getParentCount() == 0) {
                throw new IllegalArgumentException("Commit is not a merge commit or has no parents.");
            }
            RevCommit mainlineParent = mergeCommit.getParent(0);

            RefUpdate refUpdate = repo.updateRef("refs/heads/" + branchName);
            refUpdate.setNewObjectId(mainlineParent);
            refUpdate.setForceUpdate(true);

            RefUpdate.Result result = refUpdate.update();
            if (result == RefUpdate.Result.LOCK_FAILURE) {
                throw new IOException("Failed to revert merge due to a reference lock failure.");
            }
        } catch (IOException e) {
            throw new GitAPIException("In-memory merge revert failed" + e);
        }
    }

    private ObjectId createMergeCommit(Repository repository, AnyObjectId resultTreeId, RevCommit sourceCommit, RevCommit targetCommit, String commitMessage) throws IOException {
        try (ObjectInserter inserter = repository.newObjectInserter()) {
            CommitBuilder commitBuilder = new CommitBuilder();

            commitBuilder.setTreeId(resultTreeId);
            commitBuilder.setParentIds(targetCommit, sourceCommit);
            commitBuilder.setAuthor(sourceCommit.getAuthorIdent());
            commitBuilder.setCommitter(sourceCommit.getCommitterIdent());
            commitBuilder.setMessage(commitMessage);

            ObjectId mergeCommitId = inserter.insert(commitBuilder);
            inserter.flush();
            return mergeCommitId;
        }
    }

    private void updateRef(Repository repository, ObjectId mergeCommitId, ObjectId targetId, String targetBranch) throws IOException {
        RefUpdate refUpdate = repository.updateRef("refs/heads/" + targetBranch);
        refUpdate.setNewObjectId(mergeCommitId);
        refUpdate.setExpectedOldObjectId(targetId);

        RefUpdate.Result updateResult = refUpdate.update();
        if (updateResult != RefUpdate.Result.FAST_FORWARD && updateResult != RefUpdate.Result.FORCED) {
            throw new IOException("Failed to update branch reference pointer: " + updateResult);
        }
    }
}