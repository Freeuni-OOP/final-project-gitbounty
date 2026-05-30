package org.gitbounty.gitbountybackend.service.codebase.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Commit;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.commit.CommitService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitPushHook implements PostReceiveHook {

    private final BranchService branchService;
    private final CodebaseService codebaseService;
    private final CommitService commitService;
    private final CommitHistoryReader commitHistoryReader;

    private void syncPushCommandWithDb(ReceivePack receivePack, ReceiveCommand command, Codebase codebase) {
        ReceiveCommand.Type type = command.getType();
        String refName = command.getRefName();
        String branchName = (refName != null && refName.startsWith("refs/heads/"))
            ? refName.substring("refs/heads/".length())
            : refName;

        if (type == ReceiveCommand.Type.DELETE) {
            branchService.deleteBranchForCodebase(codebase, branchName);
            return;
        }

        // Security Guard: Prevent processing if forced push rejection is intended
        if (type == ReceiveCommand.Type.UPDATE_NONFASTFORWARD) {
            command.setResult(ReceiveCommand.Result.REJECTED_NONFASTFORWARD,
                "Force-pushing is disabled on GitBounty to preserve contribution history.");
            return;
        }

        ObjectId newId = command.getNewId();
        ObjectId oldId = command.getOldId();

        if (newId == null || ObjectId.zeroId().equals(newId)) {
            return;
        }

        Repository repo = receivePack.getRepository();
        Commit latestCommit = null;

        List<RevCommit> commitsToPersist = commitHistoryReader.readCommits(repo, newId, oldId);

        for (RevCommit commit : commitsToPersist) {
            String commitHash = commit.getName();
            String authorName = commit.getAuthorIdent().getName();
            String authorEmail = commit.getAuthorIdent().getEmailAddress();
            String commitMessage = commit.getFullMessage();
            Instant commitTime = commit.getCommitTime() != 0
                ? Instant.ofEpochSecond(commit.getCommitTime())
                : Instant.now();

            latestCommit = commitService.persistCommitIfMissing(
                codebase, commitHash, authorName, authorEmail, commitMessage, commitTime
            );
        }

        if (latestCommit != null) {
            if (type == ReceiveCommand.Type.CREATE) {
                branchService.createNewBranchForCodebase(codebase, branchName, latestCommit);
            } else if (type == ReceiveCommand.Type.UPDATE) {
                branchService.updateBranchLatestCommit(codebase, branchName, latestCommit);
            }
        }
    }

    @Override
    public void onPostReceive(ReceivePack receivePack, Collection<ReceiveCommand> collection) {
        if (receivePack == null || collection == null || collection.isEmpty()) {
            return;
        }

        String repoName = receivePack.getRepository().getDirectory().getName();
        Codebase codebase = codebaseService.getCodebase(repoName);

        for (ReceiveCommand command : collection) {
            syncPushCommandWithDb(receivePack, command, codebase);
        }
    }
}