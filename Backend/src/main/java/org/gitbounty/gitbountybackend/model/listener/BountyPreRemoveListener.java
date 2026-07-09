package org.gitbounty.gitbountybackend.model.listener;

import jakarta.persistence.PreRemove;
import org.gitbounty.gitbountybackend.config.ApplicationContextProvider;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;

/**
 * Registered on Bounty via @EntityListeners. Fires whenever a Bounty row is about to be
 * removed - whether directly, or cascaded from deleting its owning Issue (Bounty is the
 * one-to-one owning side of that relationship, via Bounty.issue) - and refunds it first if
 * it was still active, so escrowed funds are never silently destroyed by a cascading delete.
 *
 * JPA instantiates this class itself (via a no-arg constructor), never Spring, so it can't
 * use constructor/field injection. This codebase has no AspectJ load-time weaving and no
 * prior static-context-holder precedent (checked before adding one), so it looks up
 * BountyService lazily through ApplicationContextProvider at call time instead. The actual
 * decision logic is split into a package-visible static method so it can be unit-tested
 * directly with a mocked BountyService, without needing a live ApplicationContext.
 */
public class BountyPreRemoveListener {

    @PreRemove
    public void beforeRemove(Bounty bounty) {
        cancelIfActive(bounty, ApplicationContextProvider.getBean(BountyService.class));
    }

    static void cancelIfActive(Bounty bounty, BountyService bountyService) {
        if (bounty.getStatus() != BountyStatus.OPEN && bounty.getStatus() != BountyStatus.ASSIGNED) {
            return;
        }

        // Deliberately refundEscrowedBounty, not cancelBounty: cancelBounty also saves the
        // bounty's new status, and Bounty.issue is cascade=ALL, so that save would cascade
        // back onto the very Issue that's mid-deletion in this same flush.
        //
        // The refund transaction references this bounty for the audit trail, but the row is
        // about to disappear - sever that reference immediately so it doesn't violate
        // transactions.bounty_id's foreign key once the delete actually executes.
        Transaction refund = bountyService.refundEscrowedBounty(bounty);
        refund.setBounty(null);
    }
}
