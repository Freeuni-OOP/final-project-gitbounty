package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.gitbounty.gitbountybackend.exception.DatabaseTransactionException;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.exception.PRNotFoundException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.gitbounty.gitbountybackend.service.codebase.issue.event.IssueClosedEvent;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTests {

    @Mock private PullRequestRepository pullRequestRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private UserService userService;
    @Mock private IssueRepository issueRepository;
    @Mock private CodebaseService codebaseService;
    @Mock private GitService gitService;
    @Mock private PullRequestPersistenceService persistenceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PullRequestService pullRequestService;

    private User mockUser;
    private Codebase mockCodebase;
    private Branch mockSourceBranch;
    private Branch mockTargetBranch;
    private PullRequest mockPullRequest;

    private final String mockKeycloakId = "keycloak-user-123";
    private final String mockRepoName = "repo";
    private final Integer prNumber = 42;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("bounty-operator");
        mockUser.setEmail("operator@gitbounty.org");

        mockCodebase = new Codebase();
        mockCodebase.setId(10L);
        mockCodebase.setName(mockRepoName);

        mockSourceBranch = new Branch();
        mockSourceBranch.setName("feature-branch");

        mockTargetBranch = new Branch();
        mockTargetBranch.setName("main");

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(100L);
        mockPullRequest.setSourceBranch(mockSourceBranch);
        mockPullRequest.setTargetBranch(mockTargetBranch);
    }

    @Test
    void createPullRequest_Success() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.getCodebase(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "refs/heads/feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "refs/heads/main")).thenReturn(Optional.of(mockTargetBranch));
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.of(5));

        var command = new CreatePullRequestCommand(mockRepoName, mockKeycloakId, "feature-branch", "main", "Title", "Desc");
        when(persistenceService.create(any(), any(), any(), any(), any(), anyInt())).thenReturn(new PullRequest());

        pullRequestService.createPullRequest(command);

        verify(persistenceService).create(eq(command), any(), any(), any(), any(), eq(6));
    }

    @Test
    void mergePullRequest_Success() throws Exception {
        MergeResult mockResult = mock(MergeResult.class);
        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setSourceBranch(mockSourceBranch);
        pr.setTargetBranch(mockTargetBranch);
        pr.setAuthor(mockUser);

        when(codebaseService.getCodebase(mockRepoName)).thenReturn(mockCodebase);
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, 1)).thenReturn(Optional.of(pr));

        when(gitService.runLocked(eq(mockRepoName), any())).thenAnswer(i -> {
            GitService.SupplierWithException<?> lambda = i.getArgument(1);
            return lambda.get();
        });

        when(gitService.mergeBranches(anyString(), anyString(), anyString(), any(PersonIdent.class))).thenReturn(mockResult);

        pullRequestService.mergePullRequestForCodebase(mockRepoName, 1, mockKeycloakId);

        verify(persistenceService).finalizeMerge(99L);
        verify(eventPublisher).publishEvent(IssueClosedEvent.completed(99L, 1L));
    }

    @Test
    void mergePullRequest_RollbackTriggered_WhenDatabaseFails() throws Exception {
        MergeResult mockResult = mock(MergeResult.class);
        ObjectId commitId = ObjectId.fromString("1234567890abcdef1234567890abcdef12345678");
        when(mockResult.getNewHead()).thenReturn(commitId);

        PullRequest pr = new PullRequest();
        pr.setId(99L);
        pr.setSourceBranch(mockSourceBranch);
        pr.setTargetBranch(mockTargetBranch);

        when(codebaseService.getCodebase(mockRepoName)).thenReturn(mockCodebase);
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(pullRequestRepository.findByRepositoryAndNumber(any(), any())).thenReturn(Optional.of(pr));

        when(gitService.runLocked(eq(mockRepoName), any())).thenAnswer(i -> {
            GitService.SupplierWithException<?> lambda = i.getArgument(1);
            return lambda.get();
        });

        when(gitService.mergeBranches(anyString(), anyString(), anyString(), any(PersonIdent.class))).thenReturn(mockResult);
        when(persistenceService.finalizeMerge(99L)).thenThrow(new RuntimeException("DB Failure"));

        assertThatThrownBy(() -> pullRequestService.mergePullRequestForCodebase(mockRepoName, 1, mockKeycloakId))
            .isInstanceOf(DatabaseTransactionException.class);

        verify(gitService).revertMerge(eq(mockRepoName), eq(mockTargetBranch.getName()), eq(commitId), any(PersonIdent.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void getPullRequestsForCodebase_Success() {
        when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
        when(pullRequestRepository.findByRepository(mockCodebase)).thenReturn(List.of(new PullRequest()));

        List<PullRequest> result = pullRequestService.getPullRequestsForCodebase(mockRepoName);

        assertThat(result).hasSize(1);
        verify(codebaseService).findByName(mockRepoName);
        verify(pullRequestRepository).findByRepository(mockCodebase);
    }

    @Test
    void createPullRequest_Throws_WhenBranchesAreSame() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.getCodebase(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "refs/heads/same-branch")).thenReturn(Optional.of(mockSourceBranch));

        var command = new CreatePullRequestCommand(
            mockRepoName, mockKeycloakId, "same-branch", "same-branch", "Title", "Desc"
        );

        assertThatThrownBy(() -> pullRequestService.createPullRequest(command))
            .isInstanceOf(PRBranchesAreSameException.class);

        verifyNoInteractions(persistenceService);
    }

    @Nested
    class GetPullRequestTests {
        @Test
        void getPullRequest_ShouldReturnPR_WhenFound() {
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.of(mockPullRequest));

            PullRequest result = pullRequestService.getPullRequest(mockRepoName, prNumber);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        void getPullRequest_ShouldThrowException_WhenPRNotFound() {
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pullRequestService.getPullRequest(mockRepoName, prNumber))
                .isInstanceOf(PRNotFoundException.class);
        }
    }

    @Nested
    class UpdatePRStatusTests {
        @Test
        void updatePRStatus_ShouldInvokePersistence_WhenPRExists() {
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.of(mockPullRequest));

            pullRequestService.updatePRStatus(mockRepoName, prNumber, IssueStatus.CLOSED);

            verify(persistenceService).updatePRStatus(100L, IssueStatus.CLOSED);
            verify(eventPublisher).publishEvent(IssueClosedEvent.cancelled(100L));
        }

        @Test
        void updatePRStatus_ShouldThrowExceptionAndNotUpdate_WhenPRNotFound() {
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pullRequestService.updatePRStatus(mockRepoName, prNumber, IssueStatus.CLOSED))
                .isInstanceOf(PRNotFoundException.class);

            verifyNoInteractions(persistenceService);
        }
    }

    @Nested
    class GetPullRequestDiffTests {
        @Test
        void getPullRequestDiff_ShouldReturnDiffString_WhenPRExists() throws IOException {
            String mockDiff = "--- a/file.txt\n+++ b/file.txt";
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.of(mockPullRequest));
            when(gitService.getBranchDiff(mockRepoName, "feature-branch", "main"))
                .thenReturn(mockDiff);

            String result = pullRequestService.getPullRequestDiff(mockRepoName, prNumber);

            assertThat(result).isEqualTo(mockDiff);
            verify(gitService).getBranchDiff(mockRepoName, "feature-branch", "main");
        }

        @Test
        void getPullRequestDiff_ShouldThrowException_WhenPRNotFound() {
            when(codebaseService.findByName(mockRepoName)).thenReturn(mockCodebase);
            when(pullRequestRepository.findByRepositoryAndNumber(mockCodebase, prNumber))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pullRequestService.getPullRequestDiff(mockRepoName, prNumber))
                .isInstanceOf(PRNotFoundException.class);

            verifyNoInteractions(gitService);
        }
    }
}