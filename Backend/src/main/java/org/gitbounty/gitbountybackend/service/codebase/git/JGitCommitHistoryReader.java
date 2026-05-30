package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class JGitCommitHistoryReader implements CommitHistoryReader {
    @Override
    public List<RevCommit> readCommits(Repository repository, ObjectId newId, ObjectId oldId) {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit start = walk.parseCommit(newId);
            walk.markStart(start);

            if (oldId != null && !ObjectId.zeroId().equals(oldId)) {
                try {
                    RevCommit end = walk.parseCommit(oldId);
                    walk.markUninteresting(end);
                } catch (Exception ignored) {
                    // If the old commit can't be parsed, process reachable commits from newId anyway.
                }
            }

            List<RevCommit> commits = new ArrayList<>();
            for (RevCommit commit : walk) {
                commits.add(commit);
            }
            Collections.reverse(commits);
            return commits;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

