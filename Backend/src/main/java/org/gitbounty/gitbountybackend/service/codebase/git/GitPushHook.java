package org.gitbounty.gitbountybackend.service.codebase.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class GitPushHook implements PostReceiveHook {

    private final BranchService branchService;

    private void updateBranchTable(ReceivePack receivePack, ReceiveCommand command) {
        String refName = command.getRefName();
        String branch = (refName != null && refName.startsWith("refs/heads/"))
                ? refName.substring("refs/heads/".length())
                : refName;

        ObjectId oldId = command.getOldId(); // the last commit hash the client thinks is on the server
        ObjectId newId = command.getNewId(); // the new commit hash the client wants to push
        ReceiveCommand.Type type = command.getType();

        // Example handling (TODO: replace with actual DB interactions):
        switch (type) {
            case CREATE:
                // insert branch record with name and head = newId
                break;
            case UPDATE:
                // update branch head to newId
                break;
            case DELETE:
                // remove branch record
                break;
            case UPDATE_NONFASTFORWARD:
                // special handling if you care
                break;
            default:
                break;
        }
    }

    private void updateCommitTable(ReceivePack receivePack, ReceiveCommand command) {
        ReceiveCommand.Type type = command.getType();

        // If branch was deleted, there's no new commits to process
        if (type == ReceiveCommand.Type.DELETE) {
            //TODO: delete the corresponding branches commits
            return;
        }

        Repository repo = receivePack.getRepository();
        ObjectId newId = command.getNewId();
        ObjectId oldId = command.getOldId();

        if (newId == null || ObjectId.zeroId().equals(newId)) {
            // nothing to do cuz the new commit doesn't exist or is a null-commit (just new branch creation)
            return;
        }

        try (RevWalk walk = new RevWalk(repo)) { // this is thread-safe because we create a new instance each call
            RevCommit start = walk.parseCommit(newId);
            walk.markStart(start);

            // If oldId is non-zero, mark it uninteresting so revwalk yields only new commits
            if (oldId != null && !ObjectId.zeroId().equals(oldId)) {
                try {
                    RevCommit end = walk.parseCommit(oldId);
                    walk.markUninteresting(end);
                } catch (Exception e) {
                    // If parsing old commit fails, we still want to process reachable commits from newId
                }
            }

            for (RevCommit commit : walk) {
                // Commit info available: commit.getName(), commit.getAuthorIdent(), commit.getFullMessage(), etc.
                // TODO: persist commit into DB (author, message, date, parents, etc.)
            }
        } catch (Exception e) {
            //TODO: Handle error
        }
    }

    @Override
    public void onPostReceive(ReceivePack receivePack, Collection<ReceiveCommand> collection) {
        for (ReceiveCommand command : collection) {
            // use the full command so we can inspect refs, ids, type and result
            updateBranchTable(receivePack, command);
            updateCommitTable(receivePack, command);
        }
    }
}
