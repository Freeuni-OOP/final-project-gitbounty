package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.User.UserRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BountyServiceTest {

    @Mock private BountyRepository bountyRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BountyService bountyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    //create bounty tests

    @Test
    @DisplayName("Create Bounty: Should decrease balance and save when all inputs are valid")
    void createBounty_Success() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(1L);
        dto.setAmount(100.0);
        String userId = "jemala-uuid";

        User owner = new User();
        owner.setCreditBalance(BigDecimal.valueOf(500.0));

        Issue issue = new Issue();
        issue.setId(1L);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(userRepository.findByKeycloakId(userId)).thenReturn(Optional.of(owner));
        when(bountyRepository.save(any(Bounty.class))).thenAnswer(i -> i.getArgument(0));

        Bounty result = bountyService.createBounty(dto, userId);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(400.0), owner.getCreditBalance());
        verify(userRepository).save(owner);
        verify(bountyRepository).save(any(Bounty.class));
    }

    @Test
    @DisplayName("Create Bounty: Should throw exception when balance is too low")
    void createBounty_InsufficientFunds() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(1L);
        dto.setAmount(100.0);
        String userId = "jemala-uuid";

        User owner = new User();
        owner.setCreditBalance(BigDecimal.valueOf(50.0));

        when(issueRepository.findById(1L)).thenReturn(Optional.of(new Issue()));
        when(userRepository.findByKeycloakId(userId)).thenReturn(Optional.of(owner));

        assertThrows(IllegalArgumentException.class, () -> bountyService.createBounty(dto, userId));
        verify(bountyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Bounty: Should fail if the issue does not exist")
    void createBounty_IssueNotFound() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(999L);
        when(issueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> bountyService.createBounty(dto, "any-id"));
    }

    //state management tests

    @Test
    @DisplayName("Complete Bounty: Should close the linked issue")
    void completeBounty_ClosesIssue() {
        Bounty bounty = new Bounty();
        bounty.setStatus(BountyStatus.OPEN);
        Issue issue = new Issue();
        issue.setStatus(IssueStatus.OPEN);
        bounty.setIssue(issue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        bountyService.completeBounty(1L);

        assertEquals(BountyStatus.COMPLETED, bounty.getStatus());
        assertEquals(IssueStatus.CLOSED, issue.getStatus());
        verify(issueRepository).save(issue);
    }

    @Test
    @DisplayName("Cancel Bounty: Should refund money and update status")
    void cancelBounty_RefundsUser() {
        User mockUser = new User();
        mockUser.setKeycloakId("jemala-uuid-123");
        mockUser.setCreditBalance(BigDecimal.valueOf(100.0));

        Codebase mockCodebase = new Codebase();
        mockCodebase.setOwner(mockUser);

        Issue mockIssue = new Issue();
        mockIssue.setRepository(mockCodebase);

        Bounty mockBounty = new Bounty();
        mockBounty.setAmount(50.0);
        mockBounty.setStatus(BountyStatus.OPEN);
        mockBounty.setIssue(mockIssue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));
        when(userRepository.findByKeycloakId("jemala-uuid-123")).thenReturn(Optional.of(mockUser));

        bountyService.cancelBounty(1L);

        assertEquals(BigDecimal.valueOf(150.0), mockUser.getCreditBalance());
        assertEquals(BountyStatus.COMPLETED, mockBounty.getStatus());

        verify(userRepository).save(mockUser);
        verify(bountyRepository).save(mockBounty);
    }

    @Test
    @DisplayName("Cancel Bounty: Should not allow cancelling a completed bounty")
    void cancelBounty_FailIfAlreadyCompleted() {
        Bounty bounty = new Bounty();
        bounty.setStatus(BountyStatus.COMPLETED);
        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        assertThrows(IllegalStateException.class, () -> bountyService.cancelBounty(1L));
    }
}