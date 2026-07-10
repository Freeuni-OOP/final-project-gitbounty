package org.gitbounty.gitbountybackend.service.codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito-based unit tests for CodebaseService.deleteRepository - the thin wrapper that
 * delegates the DB cascade to CodebaseDeletionCascadeService (a genuinely separate bean, so
 * its @Transactional proxy applies correctly) and then deletes the filesystem repository
 * strictly after that transaction commits. The cascade's own internals (bounty cancellation
 * ordering, FK-safe deletion order, etc.) live in CodebaseDeletionCascadeService now and are
 * covered by CodebaseDeletionCascadeServiceTests / CodebaseDeletionCascadeServicePersistenceTests.
 */
@ExtendWith(MockitoExtension.class)
class CodebaseServiceDeletionTests {

    @Mock
    private CodebaseRepository codebaseRepository;

    @Mock
    private CodebaseStorageService codebaseStorageService;

    @Mock
    private UserService userService;

    @Mock
    private BranchService branchService;

    @Mock
    private CodebaseDeletionCascadeService codebaseDeletionCascadeService;

    private CodebaseService codebaseService;

    private Codebase codebase;

    @BeforeEach
    void setUp() {
        codebaseService = new CodebaseService(codebaseRepository, codebaseStorageService, userService, branchService,
                codebaseDeletionCascadeService);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setKeycloakId("kc-owner");

        codebase = new Codebase("demo", "desc", "url", owner);
        codebase.setId(10L);
    }

    @Test
    void deleteRepository_ShouldDeleteFilesystemRepository_AfterDbCascadeCommits() {
        when(codebaseDeletionCascadeService.deleteRepositoryRecords(10L)).thenReturn(codebase);

        Codebase result = codebaseService.deleteRepository(10L);

        assertThat(result).isEqualTo(codebase);
        // Routed through a genuinely different bean (not "this."), so the DB cascade above
        // actually goes through CodebaseDeletionCascadeService's own @Transactional proxy
        // instead of running unproxied.
        verify(codebaseDeletionCascadeService).deleteRepositoryRecords(10L);
        verify(codebaseStorageService).deleteRepository("demo");
    }

    @Test
    void deleteRepository_ShouldNotThrow_WhenFilesystemCleanupFails() {
        when(codebaseDeletionCascadeService.deleteRepositoryRecords(10L)).thenReturn(codebase);
        doThrow(new IllegalStateException("disk error")).when(codebaseStorageService).deleteRepository("demo");

        assertThat(codebaseService.deleteRepository(10L)).isEqualTo(codebase);
    }
}
