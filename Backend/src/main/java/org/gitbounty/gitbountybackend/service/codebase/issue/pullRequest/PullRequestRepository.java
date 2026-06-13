package org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    List<PullRequest> findByRepository(Codebase codebase);
}
