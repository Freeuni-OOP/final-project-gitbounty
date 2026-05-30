package org.gitbounty.gitbountybackend.service.codebase.git;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitPushHook implements PostReceiveHook {

    private final BranchService branchService;
    private final CodebaseService codebaseService;
    private final CommitService commitService;

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

        // 3. Single RevWalk Lifecycle
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit start = walk.parseCommit(newId);
            walk.markStart(start);

            if (oldId != null && !ObjectId.zeroId().equals(oldId)) {
                try {
                    RevCommit end = walk.parseCommit(oldId);
                    walk.markUninteresting(end);
                } catch (Exception e) {
                    // If parsing old commit fails, we still want to process reachable commits from newId
                }
            }

            // Collect commits into a temporary list so we can reverse the order
            List<RevCommit> commitsToPersist = new ArrayList<>();
            for (RevCommit commit : walk) {
                commitsToPersist.add(commit);
            }

            // JGit walks from newest to oldest. We reverse it so the oldest commits hit the DB first!
            Collections.reverse(commitsToPersist);

            for (RevCommit commit : commitsToPersist) {
                String commitHash = commit.getName();
                String authorName = commit.getAuthorIdent().getName();
                String authorEmail = commit.getAuthorIdent().getEmailAddress();
                String commitMessage = commit.getFullMessage();
                Instant commitTime = commit.getCommitTime() != 0
                    ? Instant.ofEpochSecond(commit.getCommitTime())
                    : Instant.now();

                // Persist commit and track the execution loop state
                latestCommit = commitService.persistCommitIfMissing(
                    codebase, commitHash, authorName, authorEmail, commitMessage, commitTime
                );
            }

            // 4. Branch pointer adjustments happen safely after all commits are fully stored
            if (latestCommit != null) {
                if (type == ReceiveCommand.Type.CREATE) {
                    branchService.createNewBranchForCodebase(codebase, branchName, latestCommit);
                } else if (type == ReceiveCommand.Type.UPDATE) {
                    branchService.updateBranchLatestCommit(codebase, branchName, latestCommit);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process Git synchronization tasks for codebase ID: {}", codebase.getId(), e);
        }
    }

    @Override
    public void onPostReceive(ReceivePack receivePack, Collection<ReceiveCommand> collection) {
        String rawFolderName = receivePack.getRepository().getDirectory().getName();
        String repoName = rawFolderName.replaceAll("\\.git$", "");
        Codebase codebase = codebaseService.getCodebase(repoName);

        for (ReceiveCommand command : collection) {
            syncPushCommandWithDb(receivePack, command, codebase);
        }
    }
}