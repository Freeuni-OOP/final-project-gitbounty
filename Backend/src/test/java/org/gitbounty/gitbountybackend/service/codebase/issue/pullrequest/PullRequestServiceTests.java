package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
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

    @Mock private PullRequestRepository pullRequestRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private UserService userService;
    @Mock private IssueRepository issueRepository;
    @Mock private CodebaseService codebaseService;

    @InjectMocks
    private PullRequestService pullRequestService;

    private User mockUser;
    private Codebase mockCodebase;
    private Branch mockSourceBranch;
    private Branch mockTargetBranch;

    private final String mockKeycloakId = "keycloak-user-123";
    private final String mockRepoName = "gitbounty-repo";

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

    private void mockSuccessfulResolution() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        // Mocking name-based lookup
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));
    }

    @Test
    void createPullRequest_Success() {
        mockSuccessfulResolution();
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.of(5));
        when(pullRequestRepository.saveAndFlush(any(PullRequest.class))).thenAnswer(i -> i.getArgument(0));

        var request = new CreatePullRequestCommand(mockRepoName, mockKeycloakId, "feature-branch", "main", "Fix bug", "Desc");
        PullRequest result = pullRequestService.createPullRequest(request);

        assertThat(result.getNumber()).isEqualTo(6);
        assertThat(result.getRepository()).isEqualTo(mockCodebase);
        verify(pullRequestRepository).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void createPullRequest_Throws_WhenCodebaseNotFound() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        // Assuming codebaseService throws a custom exception or returns null/throws
        when(codebaseService.findByName("unknown")).thenThrow(new RuntimeException("Codebase not found"));

        var request = new CreatePullRequestCommand("unknown", mockKeycloakId, "f", "m", "T", "D");

        assertThatThrownBy(() -> pullRequestService.createPullRequest(request))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createPullRequest_Throws_WhenBranchesAreSame() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));

        var request = new CreatePullRequestCommand(mockRepoName, mockKeycloakId, "main", "main", "T", "D");

        assertThatThrownBy(() -> pullRequestService.createPullRequest(request))
            .isInstanceOf(PRBranchesAreSameException.class);
    }
}