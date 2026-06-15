package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    @Mock private GitService gitService;

    @InjectMocks
    private PullRequestService pullRequestService;

    private User mockUser;
    private Codebase mockCodebase;
    private Branch mockSourceBranch;
    private Branch mockTargetBranch;

    private final String mockKeycloakId = "keycloak-user-123";
    private final String mockRepoName = "repo";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setKeycloakId(mockKeycloakId);

        mockCodebase = new Codebase();
        mockCodebase.setId(10L);
        mockCodebase.setName(mockRepoName);

        mockSourceBranch = new Branch();
        mockSourceBranch.setName("feature-branch");

        mockTargetBranch = new Branch();
        mockTargetBranch.setName("main");
    }

    @Test
    void createPullRequest_Success() {
        // Setup
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.of(5));
        when(pullRequestRepository.saveAndFlush(any(PullRequest.class))).thenAnswer(i -> i.getArgument(0));

        var command = new CreatePullRequestCommand(mockRepoName, mockKeycloakId, "feature-branch", "main", "Fix bug", "Desc");

        // Execute
        PullRequest result = pullRequestService.createPullRequest(command);

        // Verify
        assertThat(result.getTitle()).isEqualTo("Fix bug");
        assertThat(result.getNumber()).isEqualTo(6);
        verify(pullRequestRepository).saveAndFlush(any(PullRequest.class));
    }

    @Test
    void createPullRequest_Throws_WhenBranchesAreSame() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));

        var command = new CreatePullRequestCommand(mockRepoName, mockKeycloakId, "main", "main", "T", "D");

        assertThatThrownBy(() -> pullRequestService.createPullRequest(command))
            .isInstanceOf(PRBranchesAreSameException.class);
    }

    @Test
    void mergePullRequest_Success() throws Exception {
        MergeResult mockResult = mock(MergeResult.class);
        when(mockResult.getMergeStatus()).thenReturn(MergeResult.MergeStatus.FAST_FORWARD);

        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        PullRequest pr = new PullRequest();
        pr.setSourceBranch(mockSourceBranch);
        pr.setTargetBranch(mockTargetBranch);
        when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, 1)).thenReturn(Optional.of(pr));
        when(gitService.mergeBranches(mockRepoName, "feature-branch", "main")).thenReturn(mockResult);

        pullRequestService.mergePullRequestForCodebase(mockRepoName, 1);

        verify(pullRequestRepository).save(pr);
    }

    @Test
    void getPullRequestsForCodebase_Success() {
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(pullRequestRepository.findByRepository(mockCodebase)).thenReturn(List.of(new PullRequest()));

        List<PullRequest> result = pullRequestService.getPullRequestsForCodebase(mockRepoName);

        assertThat(result).hasSize(1);
        verify(codebaseService).findByName(mockRepoName);
    }
}
