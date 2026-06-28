package org.gitbounty.gitbountybackend.service.codebase.git.mergehandler;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;

public interface MergeHandler {
    MergeResult executeMerge(String sourceBranch, String targetBranch);
    void revertMerge(String branchName, ObjectId mergeCommitId);
}
