package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    List<PullRequest> findByRepository(Codebase codebase);

    Optional<PullRequest> findByRepositoryAndNumber(Codebase codebase, Integer prNumber);
}
