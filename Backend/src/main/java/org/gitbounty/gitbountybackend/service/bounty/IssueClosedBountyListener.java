package org.gitbounty.gitbountybackend.service.bounty;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.event.IssueClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IssueClosedBountyListener {

    private final BountyRepository bountyRepository;
    private final BountyService bountyService;

    public IssueClosedBountyListener(BountyRepository bountyRepository, BountyService bountyService) {
        this.bountyRepository = bountyRepository;
        this.bountyService = bountyService;
    }

    /**
     * Pays or refunds an active bounty after its issue closes.
     */
    @EventListener
    public void handleIssueClosed(IssueClosedEvent event) {
        bountyRepository.findByIssueId(event.issueId()).filter(this::isActiveBounty)
                .ifPresent(bounty -> settleBounty(bounty, event));
    }

    /**
     * Chooses between paying accepted work and refunding closed work.
     */
    private void settleBounty(Bounty bounty, IssueClosedEvent event) {
        if (event.shouldPayBounty()) {
            bountyService.completeBounty(bounty, event.bountyRecipientId());
            return;
        }
        bountyService.cancelBounty(bounty);
    }

    private boolean isActiveBounty(Bounty bounty) {
        return bounty.getStatus() == BountyStatus.OPEN || bounty.getStatus() == BountyStatus.ASSIGNED;
    }
}