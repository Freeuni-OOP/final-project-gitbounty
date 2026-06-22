package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.User.UserRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
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

    @Mock //"fake" repository
    private BountyRepository bountyRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks //put the repo in our service
    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBounty_SaveBounty_WhenAmountPositiveAndEnoughFunds() {
        //data inputs
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(10L);
        dto.setTitle("Fixx Bug");
        dto.setDescription("Fix it PLeAsEeE");
        dto.setAmount(100.0);
        String mockKeycloakId = "jemal-uuid-123";

        Issue mockIssue = new Issue();
        mockIssue.setId(10L);

        User mockOwner = new User();
        mockOwner.setKeycloakId(mockKeycloakId);
        mockOwner.setCreditBalance(BigDecimal.valueOf(500.0)); //enough

        Bounty expectedBounty = new Bounty();
        expectedBounty.setTitle(dto.getTitle());
        expectedBounty.setAmount(dto.getAmount());

        when(issueRepository.findById(10L)).thenReturn(Optional.of(mockIssue));
        when(userRepository.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockOwner));
        when(bountyRepository.save(any(Bounty.class))).thenReturn(expectedBounty);

        Bounty savedBounty = bountyService.createBounty(dto, mockKeycloakId);

        assertNotNull(savedBounty);
        assertEquals("Fixx Bug", savedBounty.getTitle());

        //500-100
        assertEquals(BigDecimal.valueOf(400.0), mockOwner.getCreditBalance());

        //verify the changes were saved
        verify(userRepository, times(1)).save(mockOwner);
        verify(bountyRepository, times(1)).save(any(Bounty.class));
    }

    @Test
    void createBounty_Exception_WhenNotEnoughFunds() {
        BountyDTO dto = new BountyDTO();
        dto.setIssueId(10L);
        dto.setAmount(100.0);
        String mockKeycloakId = "user-uuid-123";

        Issue mockIssue = new Issue();
        User mockOwner = new User();
        mockOwner.setCreditBalance(BigDecimal.valueOf(20.0)); //poor bastard

        when(issueRepository.findById(10L)).thenReturn(Optional.of(mockIssue));
        when(userRepository.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockOwner));

        // Act & Assert matching validations
        assertThrows(IllegalArgumentException.class, () -> {
bountyService.createBounty(dto, mockKeycloakId);
        });

        //make sure nothing was written down
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
    void closeIssueAndBounty_ShouldAlsoCloseLinkedIssue() {
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
}