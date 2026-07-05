package org.gitbounty.gitbountybackend.service.bounty;

import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.exception.*;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BountyServiceTest {

    @Mock
    private BountyRepository bountyRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private IssueService issueService;

    @InjectMocks
    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBounty_SaveBounty_WhenAmountPositiveAndEnoughFunds() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(10L);
        dto.setTitle("Fixx Bug");
        dto.setDescription("Fix it PLeAsEeE");
        dto.setAmount(100.0);
        String mockKeycloakId = "jemala-uuid-123";

        Issue mockIssue = new Issue();
        mockIssue.setId(10L);
        mockIssue.setNumber(42);
        mockIssue.setTitle("Fixx Bug");

        User mockOwner = new User();
        mockOwner.setKeycloakId(mockKeycloakId);
        mockOwner.setCreditBalance(BigDecimal.valueOf(500.0));

        Bounty expectedBounty = new Bounty();
        expectedBounty.setTitle(dto.getTitle());
        expectedBounty.setAmount(dto.getAmount());

        when(issueRepository.findById(10L)).thenReturn(Optional.of(mockIssue));
        when(userRepository.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockOwner));
        when(bountyRepository.save(any(Bounty.class))).thenReturn(expectedBounty);

        Bounty savedBounty = bountyService.createBounty(dto, mockKeycloakId);

        assertNotNull(savedBounty);
        assertEquals("Fixx Bug", savedBounty.getTitle());

        verify(bountyRepository, times(1)).save(any(Bounty.class));

        verify(transactionService).recordBountyDeposit(
                eq(mockOwner),
                eq(savedBounty),
                eq(BigDecimal.valueOf(100.0)),
                anyString()
        );
    }

    @Test
    void createBounty_Exception_WhenNotEnoughFunds() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(10L);
        dto.setAmount(100.0);
        String mockKeycloakId = "jemala-uuid-123";

        Issue mockIssue = new Issue();
        User mockOwner = new User();
        mockOwner.setCreditBalance(BigDecimal.valueOf(20.0));

        when(issueRepository.findById(10L)).thenReturn(Optional.of(mockIssue));
        when(userRepository.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockOwner));

        assertThrows(IllegalArgumentException.class, () -> {
            bountyService.createBounty(dto, mockKeycloakId);
        });

        verify(userRepository, never()).save(any());
        verify(bountyRepository, never()).save(any());
        verify(transactionService, never()).recordBountyDeposit(any(), any(), any(), any());
    }

    @Test
    void createBounty_ThrowsException_WhenIssueIsClosed() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(10L);
        dto.setTitle("Closed issue bounty");
        dto.setDescription("Should not be created");
        dto.setAmount(100.0);

        Issue closedIssue = new Issue();
        closedIssue.setId(10L);
        closedIssue.setStatus(IssueStatus.CLOSED);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(closedIssue));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bountyService.createBounty(dto, "user-id")
        );

        assertEquals("Bounties can only be created for open issues.", exception.getMessage());

        verify(bountyRepository, never()).save(any(Bounty.class));
        verify(transactionService, never()).recordBountyDeposit(any(), any(), any(), any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getBountyById_ShouldReturnBounty_WhenIdExists() {
        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setTitle("Task");
        bounty.setDescription("Complete this task");
        bounty.setAmount(50.0);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));
        BountyDTO found = bountyService.getBountyById(1L);

        assertNotNull(found);
        assertEquals("Task", found.getTitle());
        assertEquals("Complete this task", found.getDescription());
        assertEquals(50.0, found.getAmount());
    }

    @Test
    void getBountyById_ShouldThrowException_WhenIdDoesNotExist() {
        when(bountyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BountyNotFoundException.class, () -> {
            bountyService.getBountyById(99L);
        });
    }


    @Test
    void cancelBounty_ShouldRefundUserAndCancelBounty_WhenBountyNotCompleted() {
        User mockOwner = new User();
        mockOwner.setKeycloakId("jemala-uuid");
        mockOwner.setCreditBalance(BigDecimal.valueOf(100.0));

        Codebase mockCodebase = new Codebase();
        mockCodebase.setOwner(mockOwner);

        Issue mockIssue = new Issue();
        mockIssue.setNumber(42);
        mockIssue.setTitle("Fixx Bug");
        mockIssue.setRepository(mockCodebase);

        Bounty mockBounty = new Bounty();
        mockBounty.setId(1L);
        mockBounty.setAmount(150.0);
        mockBounty.setStatus(BountyStatus.OPEN);
        mockBounty.setIssue(mockIssue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));
        when(userRepository.findByKeycloakId("jemala-uuid")).thenReturn(Optional.of(mockOwner));

        bountyService.cancelBounty(1L);

        assertEquals(BountyStatus.CANCELLED, mockBounty.getStatus());

        verify(bountyRepository, times(1)).save(mockBounty);
        verify(transactionService).recordBountyRefund(
                eq(mockOwner),
                eq(mockBounty),
                eq(BigDecimal.valueOf(150.0)),
                contains("Bounty refund for issue #42")
        );
    }

    @Test
    void cancelBounty_ShouldThrowException_WhenBountyAlreadyCompleted() {
        Bounty mockBounty = new Bounty();
        mockBounty.setId(1L);
        mockBounty.setStatus(BountyStatus.COMPLETED);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));

        assertThrows(BountyAlreadyCompletedException.class, () -> {
            bountyService.cancelBounty(1L);
        });

        verify(userRepository, never()).save(any());
        verify(bountyRepository, never()).save(any(Bounty.class));
    }

    @Test
    void cancelBounty_ShouldNotRefundAgain_WhenAlreadyCancelled() {
        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setStatus(BountyStatus.CANCELLED);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        assertThrows(
                IllegalArgumentException.class,
                () -> bountyService.cancelBounty(1L)
        );

        verify(transactionService, never()).recordBountyRefund(any(), any(), any(), any());
    }

    @Test
    void completeBounty_ShouldPayRecipientAndMarkBountyCompleted() {
        User recipient = new User();
        recipient.setId(2L);

        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setStatus(BountyStatus.OPEN);
        bounty.setAmount(50.0);

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        bountyService.completeBounty(bounty, 2L);

        verify(transactionService).releaseBounty(bounty, recipient);

        assertEquals(BountyStatus.COMPLETED, bounty.getStatus());

        verify(bountyRepository).save(bounty);
    }

    @Test
    void claimBounty_ShouldAssignIssueAndMarkBountyAssigned() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("jemala");
        owner.setKeycloakId("jemala's kc");

        User claimant = new User();
        claimant.setId(2L);
        claimant.setUsername("cooler jemala");
        claimant.setKeycloakId("cooler jemala's kc");

        Codebase codebase = new Codebase();
        codebase.setOwner(owner);

        Issue issue = new Issue();
        issue.setId(10L);
        issue.setStatus(IssueStatus.OPEN);
        issue.setRepository(codebase);

        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setTitle("Fix bug");
        bounty.setAmount(100.0);
        bounty.setStatus(BountyStatus.OPEN);
        bounty.setIssue(issue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));
        when(userRepository.findByKeycloakId("cooler jemala's kc")).thenReturn(Optional.of(claimant));
        when(issueService.claimIssue(issue, claimant)).thenAnswer(invocation -> {
            issue.setAssignedTo(claimant);
            return issue;
        });
        when(bountyRepository.save(bounty)).thenReturn(bounty);

        BountyDTO result = bountyService.claimBounty(1L, "cooler jemala's kc");

        assertEquals(BountyStatus.ASSIGNED, result.getStatus());
        assertEquals(2L, result.getAssignedToId());
        assertEquals("cooler jemala", result.getAssignedToUsername());

        verify(issueService).claimIssue(issue, claimant);
        verify(bountyRepository).save(bounty);
    }

    @Test
    void claimBounty_ShouldRejectAlreadyAssignedBounty() {
        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setStatus(BountyStatus.ASSIGNED);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        assertThrows(
                IllegalArgumentException.class,
                () -> bountyService.claimBounty(1L, "jemala's kc")
        );

        verifyNoInteractions(issueService);
        verify(bountyRepository, never()).save(any(Bounty.class));
    }

    @Test
    void unclaimBounty_ShouldClearIssueAssigneeAndMarkBountyOpen() {
        User claimant = new User();
        claimant.setId(2L);
        claimant.setUsername("jemala");
        claimant.setKeycloakId("kc-jemala");

        Issue issue = new Issue();
        issue.setId(10L);
        issue.setStatus(IssueStatus.OPEN);
        issue.setAssignedTo(claimant);

        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setTitle("Fix bug");
        bounty.setAmount(100.0);
        bounty.setStatus(BountyStatus.ASSIGNED);
        bounty.setIssue(issue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));
        when(userRepository.findByKeycloakId("kc-jemala")).thenReturn(Optional.of(claimant));

        when(issueService.unclaimIssue(issue, claimant)).thenAnswer(invocation -> {
            issue.setAssignedTo(null);
            return issue;
        });

        when(bountyRepository.save(bounty)).thenReturn(bounty);

        BountyDTO result = bountyService.unclaimBounty(1L, "kc-jemala");

        assertEquals(BountyStatus.OPEN, result.getStatus());
        assertNull(result.getAssignedToId());
        assertNull(result.getAssignedToUsername());

        verify(issueService).unclaimIssue(issue, claimant);
        verify(bountyRepository).save(bounty);
    }

    @Test
    void unclaimBounty_ShouldRejectOpenBounty() {
        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setStatus(BountyStatus.OPEN);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        assertThrows(
                IllegalArgumentException.class,
                () -> bountyService.unclaimBounty(1L, "kc-jemala")
        );

        verifyNoInteractions(issueService);
        verify(bountyRepository, never()).save(any(Bounty.class));
    }

    @Test
    void unclaimBounty_ShouldRejectWhenUserIsNotAssignedClaimant() {
        User assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setUsername("jemala");
        assignedUser.setKeycloakId("kc-jemala");

        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("cooler jemala");
        otherUser.setKeycloakId("kc-cooler jemala");

        Issue issue = new Issue();
        issue.setId(10L);
        issue.setStatus(IssueStatus.OPEN);
        issue.setAssignedTo(assignedUser);

        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setStatus(BountyStatus.ASSIGNED);
        bounty.setIssue(issue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));
        when(userRepository.findByKeycloakId("kc-cooler jemala")).thenReturn(Optional.of(otherUser));

        when(issueService.unclaimIssue(issue, otherUser))
                .thenThrow(new IllegalArgumentException("Only the assigned user can leave this issue."));

        assertThrows(
                IllegalArgumentException.class,
                () -> bountyService.unclaimBounty(1L, "kc-cooler jemala")
        );

        verify(issueService).unclaimIssue(issue, otherUser);
        verify(bountyRepository, never()).save(any(Bounty.class));
    }


}