package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
class GitService {

    public MergeResult mergeBranches(String repoPath, String sourceBranch, String targetBranch) throws IOException, GitAPIException {
        File repoDir = new File(repoPath);

        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(new File(repoDir, ".git"))
            .readEnvironment()
            .findGitDir()
            .build();
             Git git = new Git(repository)) {

            // 1. Checkout the target branch
            git.checkout().setName(targetBranch).call();

            // 2. Perform the merge
            // We refer to the source branch using the ref name
            return git.merge()
                .include(repository.findRef(sourceBranch))
                .setCommit(true)
                .setMessage("Merge " + sourceBranch + " into " + targetBranch)
                .call();
        }
    }
}
