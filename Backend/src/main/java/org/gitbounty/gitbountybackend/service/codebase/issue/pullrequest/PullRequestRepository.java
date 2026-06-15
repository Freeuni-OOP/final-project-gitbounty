package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
}
