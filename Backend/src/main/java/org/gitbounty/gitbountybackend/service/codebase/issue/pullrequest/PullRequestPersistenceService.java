package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.exception.ResourceNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PullRequestPersistenceService {

    private final PullRequestRepository repository;
    private final BountyService bountyService;

    PullRequestPersistenceService(PullRequestRepository repository, BountyService bountyService) {
        this.repository = repository;
        this.bountyService = bountyService;
    }

    /**
     * Creates and stores a new pull request.
     */
    public PullRequest create(CreatePullRequestCommand command, User author, Codebase codebase,
                                Branch source, Branch target, Integer number) {
        PullRequest pr = new PullRequest();
        pr.setTitle(PullRequest.normalizeTitle(command.title()));
        pr.setDescription(PullRequest.normalizeDescription(command.description()));
        pr.setNumber(number);
        pr.setAuthor(author);
        pr.setRepository(codebase);
        pr.setSourceBranch(source);
        pr.setTargetBranch(target);

        return repository.saveAndFlush(pr);
    }

    /**
     * Finalizes a successful Git merge.
     *
     * The PR closes, and an active bounty is paid to the PR author.
     * All database changes happen together, so a failure rolls them all back.
     */
    @Transactional
    public PullRequest finalizeMerge(Long prId) {
        PullRequest pr = findPullRequest(prId);

        if (pr.getMergedAt() != null) {
            throw new IllegalArgumentException("Pull request is already merged: " + prId);
        }

        if (pr.getStatus() == IssueStatus.CLOSED) {
            throw new IllegalArgumentException("A closed pull request cannot be merged: " + prId);
        }

        Bounty bounty = pr.getBounty();

        if (isActiveBounty(bounty)) {
            bountyService.completeBountyAndPayRecipient(bounty.getId(), pr.getAuthor());
        }

        pr.setStatus(IssueStatus.CLOSED);
        pr.setMergedAt(Instant.now());

        return repository.saveAndFlush(pr);
    }

    /**
     * Deletes the stored pull-request.
     */
    public void delete(Long prId) {
        repository.deleteById(prId);
    }

    /**
     * Changes the PR status.
     *
     * Closing without merging cancels and refunds an active bounty,
     * because no contributor's work was approved.
     */
    @Transactional
    public void updatePRStatus(Long id, IssueStatus issueStatus) {
        PullRequest pr = findPullRequest(id);

        if (issueStatus == IssueStatus.CLOSED && pr.getMergedAt() == null && isActiveBounty(pr.getBounty())) {
            bountyService.cancelBounty(pr.getBounty().getId());
        }

        pr.setStatus(issueStatus);
        repository.saveAndFlush(pr);
    }

    /**
     * Loads a PR.
     */
    private PullRequest findPullRequest(Long prId) {
        return repository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("PR not found with ID: " + prId));
    }

    /**
     * Returns true when the bounty can still be paid or refunded.
     */
    private boolean isActiveBounty(Bounty bounty) {
        return bounty != null
                && (bounty.getStatus() == BountyStatus.OPEN
                        || bounty.getStatus()
                        == BountyStatus.ASSIGNED
        );
    }
}