package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BountyServiceTest {

    @Mock //"fake" repository
    private BountyRepository bountyRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks //put the repo in our service
    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBounty_SaveBounty_WhenAmountPositive() {
        Bounty bounty = new Bounty("Fixx Bug", "Fix it please", 100.0, BountyStatus.OPEN);
        when(bountyRepository.save(any(Bounty.class))).thenReturn(bounty);

        //call the method we are testing
        Bounty savedBounty = bountyService.createBounty(bounty);

        //check that it worked and the save method was called
        assertNotNull(savedBounty);
        assertEquals("Fixx Bug", savedBounty.getTitle());
        verify(bountyRepository, times(1)).save(bounty);
    }

    @Test
    void createBounty_Exception_WhenAmountIsBad() {
        //bounty with invalid amount
        Bounty invalidBounty = new Bounty("Cheap Bug", "Too cheap", -10.0, BountyStatus.OPEN);

        //should be IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bountyService.createBounty(invalidBounty);
        });

        //check that save method was not called
        verify(bountyRepository, never()).save(any());
    }

    @Test
    void getBountyById_ShouldReturnBounty_WhenIdExists() {
        Bounty bounty = new Bounty("Task", "Desc", 50.0, BountyStatus.OPEN);
        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        BountyDTO found = bountyService.getBountyById(1L);

        assertEquals("Task", found.getTitle());
    }

    @Test
    void closeBounty_ShouldAlsoCloseLinkedIssue() {
        Issue mockIssue = new Issue();
        mockIssue.setStatus(IssueStatus.OPEN);

        Bounty mockBounty = new Bounty();
        mockBounty.setId(1L);
        mockBounty.setStatus(BountyStatus.OPEN);
        mockBounty.setIssue(mockIssue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));

        bountyService.closeBounty(1L);

        assertEquals(BountyStatus.COMPLETED, mockBounty.getStatus());
        assertEquals(IssueStatus.CLOSED, mockIssue.getStatus(), "The linked issue should be closed when bounty is completed");

        verify(issueRepository, times(1)).save(mockIssue);
    }

    @Test
    void closeIssue_ShouldCloseLinkedBounty() {
        Bounty mockBounty = new Bounty();
        mockBounty.setStatus(BountyStatus.OPEN);

        when(bountyRepository.findByIssueId(1L)).thenReturn(Optional.of(mockBounty));

        bountyService.closeIssue(1L);

        assertEquals(BountyStatus.COMPLETED, mockBounty.getStatus());
        verify(bountyRepository, times(1)).save(mockBounty);
    }
}