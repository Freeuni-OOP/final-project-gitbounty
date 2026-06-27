package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.exception.*;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        assertEquals(BigDecimal.valueOf(400.0), mockOwner.getCreditBalance());

        verify(userRepository, times(1)).save(mockOwner);
        verify(bountyRepository, times(1)).save(any(Bounty.class));
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
    }

    @Test
    void getBountyById_ShouldReturnBounty_WhenIdExists() {
        Bounty bounty = new Bounty();
        bounty.setId(1L);
        bounty.setTitle("Task");
        bounty.setAmount(50.0);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));
        BountyDTO found = bountyService.getBountyById(1L);

        assertNotNull(found);
        assertEquals("Task", found.getTitle());
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
    void completeBounty_ShouldCompleteBountyAndCloseLinkedIssue() {
        Issue mockIssue = new Issue();
        mockIssue.setStatus(IssueStatus.OPEN);

        Bounty mockBounty = new Bounty();
        mockBounty.setId(1L);
        mockBounty.setStatus(BountyStatus.OPEN);
        mockBounty.setIssue(mockIssue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));

        bountyService.completeBounty(1L);

        assertEquals(BountyStatus.COMPLETED, mockBounty.getStatus());
        assertEquals(IssueStatus.CLOSED, mockIssue.getStatus());

        verify(bountyRepository, times(1)).save(mockBounty);
        verify(issueRepository, times(1)).save(mockIssue);
    }

    @Test
    void closeIssueAndBounty_ShouldCloseIssueAndCompleteLinkedBounty() {
        Issue mockIssue = new Issue();
        mockIssue.setId(20L);
        mockIssue.setStatus(IssueStatus.OPEN);

        Bounty mockBounty = new Bounty();
        mockBounty.setStatus(BountyStatus.OPEN);

        when(issueRepository.findById(20L)).thenReturn(Optional.of(mockIssue));
        when(bountyRepository.findByIssueId(20L)).thenReturn(Optional.of(mockBounty));

        bountyService.closeIssueAndBounty(20L);

        assertEquals(IssueStatus.CLOSED, mockIssue.getStatus());
        assertEquals(BountyStatus.COMPLETED, mockBounty.getStatus());

        verify(issueRepository, times(1)).save(mockIssue);
        verify(bountyRepository, times(1)).save(mockBounty);
    }

    @Test
    void cancelBounty_ShouldRefundUserAndCancelBounty_WhenBountyNotCompleted() {
        User mockOwner = new User();
        mockOwner.setKeycloakId("jemala-uuid");
        mockOwner.setCreditBalance(BigDecimal.valueOf(100.0));

        Codebase mockCodebase = new Codebase();
        mockCodebase.setOwner(mockOwner);

        Issue mockIssue = new Issue();
        mockIssue.setRepository(mockCodebase);

        Bounty mockBounty = new Bounty();
        mockBounty.setId(1L);
        mockBounty.setAmount(150.0);
        mockBounty.setStatus(BountyStatus.OPEN);
        mockBounty.setIssue(mockIssue);

        when(bountyRepository.findById(1L)).thenReturn(Optional.of(mockBounty));
        when(userRepository.findByKeycloakId("jemala-uuid")).thenReturn(Optional.of(mockOwner));

        bountyService.cancelBounty(1L);

        assertEquals(BigDecimal.valueOf(250.0), mockOwner.getCreditBalance());
        assertEquals(BountyStatus.CANCELLED, mockBounty.getStatus());

        verify(userRepository, times(1)).save(mockOwner);
        verify(bountyRepository, times(1)).save(mockBounty);
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
}