package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public interface CommitHistoryReader {
    List<RevCommit> readCommits(Repository repository, ObjectId newId, ObjectId oldId);
}

