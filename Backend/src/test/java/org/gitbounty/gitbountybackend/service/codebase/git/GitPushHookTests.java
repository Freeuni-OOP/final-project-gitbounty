package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Commit;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.commit.CommitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GitPushHookTests {

    @Mock
    BranchService branchService;

    @Mock
    CodebaseService codebaseService;

    @Mock
    CommitService commitService;

    @Mock
    CommitHistoryReader commitHistoryReader;

    @InjectMocks
    GitPushHook gitPushHook;

    private ReceivePack receivePack;

    private ReceivePack mockReceivePack(String repositoryDirectoryName) {
        Repository repository = mock(Repository.class);
        when(repository.getDirectory()).thenReturn(new java.io.File(repositoryDirectoryName));

        ReceivePack pack = mock(ReceivePack.class);
        when(pack.getRepository()).thenReturn(repository);
        return pack;
    }

    private RevCommit mockCommit(String hash, String authorName, String authorEmail, String message, int commitTime) {
        RevCommit commit = mock(RevCommit.class);
        PersonIdent ident = mock(PersonIdent.class);
        when(commit.getName()).thenReturn(hash);
        when(commit.getAuthorIdent()).thenReturn(ident);
        when(ident.getName()).thenReturn(authorName);
        when(ident.getEmailAddress()).thenReturn(authorEmail);
        when(commit.getFullMessage()).thenReturn(message);
        when(commit.getCommitTime()).thenReturn(commitTime);
        return commit;
    }

    @Test
    void deleteCommand_invokesDeleteBranch() {
        receivePack = mockReceivePack("myrepo.git");

        Codebase codebase = new Codebase();
        codebase.setId(5L);
        when(codebaseService.getCodebase("myrepo.git")).thenReturn(codebase);

        ReceiveCommand cmd = new ReceiveCommand(
                ObjectId.fromString("0000000000000000000000000000000000000000"),
                ObjectId.zeroId(),
                "refs/heads/feature-x",
                ReceiveCommand.Type.DELETE
        );

        gitPushHook.onPostReceive(receivePack, List.of(cmd));

        verify(branchService, times(1)).deleteBranchForCodebase(codebase, "feature-x");
    }

    @Test
    void nonFastForward_isRejected() {
        receivePack = mockReceivePack("myrepo.git");

        Codebase codebase = new Codebase();
        codebase.setId(6L);
        when(codebaseService.getCodebase("myrepo.git")).thenReturn(codebase);

        ReceiveCommand cmd = new ReceiveCommand(
                ObjectId.fromString("1111111111111111111111111111111111111111"),
                ObjectId.fromString("1111111111111111111111111111111111111111"),
                "refs/heads/main",
                ReceiveCommand.Type.UPDATE_NONFASTFORWARD
        );

        gitPushHook.onPostReceive(receivePack, List.of(cmd));

        assertEquals(ReceiveCommand.Result.REJECTED_NONFASTFORWARD, cmd.getResult());
        assertNotNull(cmd.getMessage());
        verifyNoInteractions(branchService);
    }

    @Test
    void createCommand_callsCreateBranch_whenNewCommitExists() {
        receivePack = mockReceivePack("myrepo.git");

        Codebase codebase = new Codebase();
        codebase.setId(7L);
        when(codebaseService.getCodebase("myrepo.git")).thenReturn(codebase);

        RevCommit commit = mockCommit("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Test User", "test@example.com", "first", 1700000000);
        when(commitHistoryReader.readCommits(any(), any(), any())).thenReturn(List.of(commit));
        when(commitService.persistCommitIfMissing(eq(codebase), anyString(), anyString(), anyString(), anyString(), any()))
            .thenAnswer(inv -> Commit.builder().commitHash(inv.getArgument(1)).build());

        ReceiveCommand cmd = new ReceiveCommand(
                ObjectId.zeroId(),
                ObjectId.fromString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                "refs/heads/new-feature",
                ReceiveCommand.Type.CREATE
        );

        gitPushHook.onPostReceive(receivePack, List.of(cmd));

        verify(commitHistoryReader, times(1)).readCommits(any(), any(), any());
        verify(branchService, times(1)).createNewBranchForCodebase(eq(codebase), eq("new-feature"), any(Commit.class));
    }

    @Test
    void updateCommand_callsUpdateBranch_whenNewCommits() {
        receivePack = mockReceivePack("myrepo.git");

        Codebase codebase = new Codebase();
        codebase.setId(8L);
        when(codebaseService.getCodebase("myrepo.git")).thenReturn(codebase);

        RevCommit older = mockCommit("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "Test User", "test@example.com", "first", 1700000000);
        RevCommit newer = mockCommit("cccccccccccccccccccccccccccccccccccccccc", "Test User", "test@example.com", "second", 1700000100);
        when(commitHistoryReader.readCommits(any(), any(), any())).thenReturn(List.of(older, newer));
        when(commitService.persistCommitIfMissing(eq(codebase), anyString(), anyString(), anyString(), anyString(), any()))
            .thenAnswer(inv -> Commit.builder().commitHash(inv.getArgument(1)).build());

        ReceiveCommand cmd = new ReceiveCommand(
                ObjectId.fromString("cccccccccccccccccccccccccccccccccccccccc"),
                ObjectId.fromString("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
                "refs/heads/main",
                ReceiveCommand.Type.UPDATE
        );

        gitPushHook.onPostReceive(receivePack, List.of(cmd));

        verify(commitHistoryReader, times(1)).readCommits(any(), any(), any());
        verify(branchService, times(1)).updateBranchLatestCommit(eq(codebase), eq("main"), any(Commit.class));
    }
}
