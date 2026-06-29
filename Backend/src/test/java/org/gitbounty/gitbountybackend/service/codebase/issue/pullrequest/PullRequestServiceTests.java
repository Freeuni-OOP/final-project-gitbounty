package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.gitbounty.gitbountybackend.exception.DatabaseTransactionException;
import org.gitbounty.gitbountybackend.exception.PRBranchesAreSameException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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
    @Mock private PullRequestPersistenceService persistenceService;

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
        mockUser.setUsername("bounty-operator");
        mockUser.setEmail("operator@gitbounty.org");

        mockCodebase = new Codebase();
        mockCodebase.setId(10L);

        mockSourceBranch = new Branch();
        mockSourceBranch.setName("feature-branch");

        mockTargetBranch = new Branch();
        mockTargetBranch.setName("main");
    }

    @Test
    void createPullRequest_Success() {
        when(userService.findByKeycloakId(mockKeycloakId)).thenReturn(Optional.of(mockUser));
        when(codebaseService.getCodebase(mockRepoName)).thenReturn(mockCodebase);
        when(branchRepository.findByCodebaseIdAndName(10L, "feature-branch")).thenReturn(Optional.of(mockSourceBranch));
        when(branchRepository.findByCodebaseIdAndName(10L, "main")).thenReturn(Optional.of(mockTargetBranch));
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
        when(branchRepository.findByCodebaseIdAndName(10L, "same-branch")).thenReturn(Optional.of(mockSourceBranch));

        var command = new CreatePullRequestCommand(
            mockRepoName, mockKeycloakId, "same-branch", "same-branch", "Title", "Desc"
        );

        assertThatThrownBy(() -> pullRequestService.createPullRequest(command))
            .isInstanceOf(PRBranchesAreSameException.class);

        verifyNoInteractions(persistenceService);
    }
}