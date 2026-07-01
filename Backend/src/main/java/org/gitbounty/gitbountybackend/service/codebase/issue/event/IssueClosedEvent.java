package org.gitbounty.gitbountybackend.service.codebase.issue.event;

/**
 * Announces that an issue or pull request was closed.
 *
 * A recipient means the work was accepted and the bounty should be paid.
 * No recipient means the work was not accepted and the bounty should be refunded.
 */
public record IssueClosedEvent(Long issueId, Long bountyRecipientId) {
    /**
     * Creates an event for accepted work that should receive payment.
     */
    public static IssueClosedEvent completed(Long issueId, Long bountyRecipientId) {
        if (issueId == null) {
            throw new IllegalArgumentException("Closed issue ID is required.");
        }

        if (bountyRecipientId == null) {
            throw new IllegalArgumentException("Bounty recipient ID is required.");
        }

        return new IssueClosedEvent(issueId, bountyRecipientId);
    }

    /**
     * Creates an event for closed work whose bounty should be refunded.
     */
    public static IssueClosedEvent cancelled(Long issueId) {
        if (issueId == null) {
            throw new IllegalArgumentException("Closed issue ID is required.");
        }

        return new IssueClosedEvent(issueId, null);
    }

    /**
     * Tells the listener whether this closure should cause a payout.
     */
    public boolean shouldPayBounty() {
        return bountyRecipientId != null;
    }
}