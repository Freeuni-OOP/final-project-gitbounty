package org.gitbounty.gitbountybackend.service.codebase.issue.pullRequest;

import org.gitbounty.gitbountybackend.exception.BranchNotFoundException;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTests {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserService userService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CodebaseService codebaseService;

    @InjectMocks
    private PullRequestService pullRequestService;

    private User mockUser;
    private Codebase mockCodebase;
    private Branch mockSourceBranch;
    private Branch mockTargetBranch;

    private final String mockKeycloakId = "keycloak-user-123";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setKeycloakId(mockKeycloakId);

        mockCodebase = new Codebase();
        mockCodebase.setId(10L);

        mockSourceBranch = new Branch();
        mockSourceBranch.setName("feature-branch");

        mockTargetBranch = new Branch();
        mockTargetBranch.setName("main");
    }

    @Test
    void createPullRequest_Success_WithAllParams() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findById(10L)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.of(5));

        when(pullRequestRepository.saveAndFlush(any(PullRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PullRequest result = pullRequestService.createPullRequest(
            10L, mockKeycloakId, "feature-branch", "main", "  Fix bug  ", "Description"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Fix bug"); // Normalized
        assertThat(result.getNumber()).isEqualTo(6); // 5 + 1
        assertThat(result.getAuthor()).isEqualTo(mockUser);
        assertThat(result.getRepository()).isEqualTo(mockCodebase);
        assertThat(result.getSourceBranch()).isEqualTo(mockSourceBranch);
        assertThat(result.getTargetBranch()).isEqualTo(mockTargetBranch);

        verify(pullRequestRepository).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void createPullRequest_Throws_NoSourceBranch() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        assertThatThrownBy( () -> pullRequestService.createPullRequest(
            10L, mockKeycloakId, null, "main", "Title", "Description"
        )).isInstanceOf(IllegalArgumentException.class);

        verify(branchRepository, never()).findByCodebaseIdAndName(eq(10L), anyString());
        verify(pullRequestRepository, never()).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void createPullRequest_Throws_WhenUserIdNull() {
        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, null, "feature", "main", "Title", "Desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User id is required");

        verifyNoInteractions(pullRequestRepository);
    }

    @Test
    void createPullRequest_Throws_WhenUserNotFound() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, mockKeycloakId, "feature", "main", "Title", "Desc"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createPullRequest_Throws_WhenCodebaseIdNull() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> pullRequestService.createPullRequest((Long) null, mockKeycloakId, "feature", "main", "Title", "Desc"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPullRequest_Throws_WhenSourceBranchNotFound() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findById(10L)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, mockKeycloakId, "invalid", "main", "Title", "Desc"))
            .isInstanceOf(BranchNotFoundException.class);
    }

    @Test
    void createPullRequest_Throws_WhenTargetBranchNotFound() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findById(10L)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, mockKeycloakId, "feature-branch", "main", "Title", "Desc"))
            .isInstanceOf(BranchNotFoundException.class)
            .hasMessageContaining("branch not found: main");

        verify(branchRepository).findByCodebaseIdAndName(10L, "feature-branch");
        verify(branchRepository).findByCodebaseIdAndName(10L, "main");
        verify(pullRequestRepository, never()).saveAndFlush(any(PullRequest.class));
    }
    @Test
    void createPullRequest_Throws_WhenTargetAndSourceIsSameBranch() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findById(10L)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));

        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, mockKeycloakId, "main", "main", "Title", "Desc"))
            .isInstanceOf(PRBranchesAreSameException.class);

        verify(branchRepository, times(2)).findByCodebaseIdAndName(10L, "main");
        verify(pullRequestRepository, never()).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void createPullRequest_Throws_WhenTitleIsWhitespace(){
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findById(10L)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));

        assertThatThrownBy(() -> pullRequestService.createPullRequest(10L, mockKeycloakId, "feature-branch", "main", " ", "Desc"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(branchRepository).findByCodebaseIdAndName(10L, "main");
        verify(pullRequestRepository, never()).saveAndFlush(any(PullRequest.class));

    }
}