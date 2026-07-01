package org.gitbounty.gitbountybackend.service.bounty;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.event.IssueClosedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueClosedBountyListenerTest {

    @Mock
    private BountyRepository bountyRepository;

    @Mock
    private BountyService bountyService;

    @InjectMocks
    private IssueClosedBountyListener listener;

    @Test
    void handleIssueClosed_ShouldPayActiveBounty_WhenRecipientExists() {
        Bounty bounty = new Bounty();
        bounty.setId(5L);
        bounty.setStatus(BountyStatus.OPEN);

        when(bountyRepository.findByIssueId(10L)).thenReturn(Optional.of(bounty));

        listener.handleIssueClosed(IssueClosedEvent.completed(10L, 2L));

        verify(bountyService).completeBounty(bounty, 2L);
        verify(bountyService, never()).cancelBounty(any(Bounty.class));
    }

    @Test
    void handleIssueClosed_ShouldRefundActiveBounty_WhenNoRecipientExists() {
        Bounty bounty = new Bounty();
        bounty.setId(5L);
        bounty.setStatus(BountyStatus.ASSIGNED);

        when(bountyRepository.findByIssueId(10L)).thenReturn(Optional.of(bounty));

        listener.handleIssueClosed(IssueClosedEvent.cancelled(10L));

        verify(bountyService).cancelBounty(bounty);
        verify(bountyService, never()).completeBounty(any(), anyLong());
    }

    @Test
    void handleIssueClosed_ShouldIgnoreAlreadyCompletedBounty() {
        Bounty bounty = new Bounty();
        bounty.setId(5L);
        bounty.setStatus(BountyStatus.COMPLETED);

        when(bountyRepository.findByIssueId(10L)).thenReturn(Optional.of(bounty));

        listener.handleIssueClosed(IssueClosedEvent.completed(10L, 2L));

        verifyNoInteractions(bountyService);
    }

    @Test
    void handleIssueClosed_ShouldDoNothing_WhenIssueHasNoBounty() {
        when(bountyRepository.findByIssueId(10L)).thenReturn(Optional.empty());
        listener.handleIssueClosed(IssueClosedEvent.cancelled(10L));
        verifyNoInteractions(bountyService);
    }
}