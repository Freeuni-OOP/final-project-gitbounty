package org.gitbounty.gitbountybackend.service.codebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.service.codebase.dto.FileType;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.FileContents;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodebaseServiceTests {

    private CodebaseRepository codebaseRepository;
    private UserService userService;
    private CodebaseStorageService storageService;
    private CodebaseService codebaseService;
    private User owner;
    private BranchService branchService;

    @BeforeAll
    void initSuite() {
        codebaseRepository = Mockito.mock(CodebaseRepository.class);
        userService = Mockito.mock(UserService.class);
        storageService = Mockito.mock(CodebaseStorageService.class);
        branchService = Mockito.mock(BranchService.class);
        codebaseService = new CodebaseService(codebaseRepository, storageService, userService, branchService);
        owner = new User("git-owner", "git-owner@test.local", randomKeycloakId());
    }

    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void resetMocks() {
        reset(codebaseRepository, userService, storageService, branchService);
    }

    @Test
    void createCodebaseCreatesStorageAndPersistsCodebase() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Codebase created = codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId());

        assertThat(created.getName()).isEqualTo("demo");
        assertThat(created.getDescription()).isEqualTo("Demo repository");
        assertThat(created.getGitUrl()).isEqualTo("http://localhost/git/demo.git");
        assertThat(created.getOwner().getUsername()).isEqualTo("git-owner");
        verify(storageService).createRepository("demo");
    }

    @Test
    void createCodebaseDeletesStorageWhenPersistenceFails() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("boom");

        verify(storageService).createRepository("demo");
        verify(storageService).deleteRepository("demo");
    }

    @Test
    void createCodebaseKeepsOriginalExceptionWhenCleanupFails() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenThrow(new IllegalStateException("boom"));
        Mockito.doThrow(new RuntimeException("cleanup-failed")).when(storageService).deleteRepository("demo");

        Throwable thrown = catchThrowable(() ->
            codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId())
        );

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getSuppressed()).hasSize(1);
        assertThat(thrown.getSuppressed()[0].getMessage()).isEqualTo("cleanup-failed");
    }

    @Test
    void createCodebaseRejectsDuplicateRepositoryNames() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(
            Optional.of(new Codebase("demo", "Existing", "http://localhost/git/demo.git", owner))
        );

        assertThatThrownBy(() -> codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId()))
            .isInstanceOf(IllegalStateException.class);

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebaseRejectsPathSeparatorsInName() {
        assertThatThrownBy(() -> codebaseService.createCodebase("demo/evil", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId()))
            .isInstanceOf(IllegalArgumentException.class);

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebaseRejectsUnknownAuthenticatedUser() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", null))
            .isInstanceOf(UserNotFoundException.class);

        verify(storageService, never()).createRepository(any());
    }

    @Test
    void createCodebasePropagatesStorageConflict() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());
        Mockito.doThrow(new IllegalStateException("boom")).when(storageService).createRepository(any());

        assertThatThrownBy(() -> codebaseService.createCodebase("demo", "Demo repository", "http://localhost/git/demo.git", owner.getKeycloakId()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteCodebaseDeletesRecordAndStorage() {
        Codebase existing = new Codebase("demo", "desc", "url", owner);
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.of(existing));

        codebaseService.deleteCodebase("demo");

        verify(codebaseRepository).delete(existing);
        verify(storageService).deleteRepository("demo");
    }

    @Test
    void deleteCodebaseStillDeletesStorageWhenRecordMissing() {
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());

        codebaseService.deleteCodebase("demo");

        verify(codebaseRepository, never()).delete(any());
        verify(storageService).deleteRepository("demo");
    }

    @Test
    void deleteCodebaseRejectsInvalidName() {
        assertThatThrownBy(() -> codebaseService.deleteCodebase("../evil"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(storageService, never()).deleteRepository(any());
    }
    @Test
    void listCodebaseContents_File_ReturnsFileDTO() {
        // 1. Mock Repository existence
        when(codebaseRepository.existsByName("demo")).thenReturn(true);

        // 2. Mock Storage Service to return a FileContents
        FileContents mockFile = new FileContents("Main.java","public class Main {}");
        when(storageService.getPathContents("demo", "src/Main.java", "master"))
            .thenReturn(mockFile);

        // 3. Call service
        CodebaseContentsDTO dto = codebaseService.listCodebaseContents("demo", "src/Main.java", "master");

        // 4. Assert
        assertThat(dto.type()).isEqualTo(FileType.FILE);
        assertThat(dto.content()).isEqualTo("public class Main {}");
        assertThat(dto.items()).isNull();
    }

    @Test
    void listCodebaseContents_Directory_ReturnsDirectoryDTO() {
        // 1. Mock Repository existence
        when(codebaseRepository.existsByName("demo")).thenReturn(true);

        // 2. Mock Storage Service to return DirectoryContents
        DirectoryContents mockDir = new DirectoryContents("src/", List.of("src", "README.md"));
        when(storageService.getPathContents("demo", "src", "master"))
            .thenReturn(mockDir);

        // 3. Call service
        CodebaseContentsDTO dto = codebaseService.listCodebaseContents("demo", "src", "master");

        // 4. Assert
        assertThat(dto.type()).isEqualTo(FileType.DIRECTORY);
        assertThat(dto.items()).containsExactly("src", "README.md");
        assertThat(dto.content()).isNull();
    }

    @Test
    void listCodebaseContents_ThrowsWhenRepoMissing() {
        // 1. Mock repository missing
        when(codebaseRepository.existsByName("unknown")).thenReturn(false);

        // 2. Assert exception
        assertThatThrownBy(() -> codebaseService.listCodebaseContents("unknown", "/", "master"))
            .isInstanceOf(CodebaseNotFoundException.class);

        // 3. Verify storage was never touched
        verify(storageService, never()).getPathContents(any(), any(), any());
    }
    @Test
    void updateCodebase_updatesFieldsSuccessfully() {
        // 1. Arrange
        Codebase existing = new Codebase("demo", "Old desc", "http://old.url", owner);
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.of(existing));
        when(codebaseRepository.save(existing)).thenReturn(existing);

        UpdateCodebaseCommand command = new UpdateCodebaseCommand("new-name", "New desc", "http://new.url");

        // 2. Act
        Codebase updated = codebaseService.updateCodebase("demo", command);

        // 3. Assert
        assertThat(updated.getName()).isEqualTo("new-name");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getGitUrl()).isEqualTo("http://new.url");
        verify(codebaseRepository).save(existing); // Or verify it's updated via dirty checking
    }

    @Test
    void updateCodebase_partialUpdate_onlyChangesSpecifiedFields() {
        // 1. Arrange
        Codebase existing = new Codebase("demo", "Old desc", "http://old.url", owner);
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.of(existing));
        when(codebaseRepository.save(existing)).thenReturn(existing);

        // Only provide new description, name and gitUrl are null
        UpdateCodebaseCommand command = new UpdateCodebaseCommand(null, "New desc", null);

        // 2. Act
        codebaseService.updateCodebase("demo", command);

        // 3. Assert
        assertThat(existing.getName()).isEqualTo("demo"); // Should remain unchanged
        assertThat(existing.getDescription()).isEqualTo("New desc");
        assertThat(existing.getGitUrl()).isEqualTo("http://old.url"); // Should remain unchanged
    }

    @Test
    void updateCodebase_throwsException_whenCodebaseNotFound() {
        // 1. Arrange
        when(codebaseRepository.findByName("unknown")).thenReturn(Optional.empty());
        UpdateCodebaseCommand command = new UpdateCodebaseCommand("new", "desc", "url");

        // 2. Act & Assert
        assertThatThrownBy(() -> codebaseService.updateCodebase("unknown", command))
            .isInstanceOf(CodebaseNotFoundException.class);
    }

    @Test
    void createCodebaseDeletesStorageWhenDefaultBranchCreationFails() {
        when(userService.findByKeycloakId(owner.getKeycloakId())).thenReturn(Optional.of(owner));
        when(codebaseRepository.findByName("demo")).thenReturn(Optional.empty());
        when(codebaseRepository.saveAndFlush(any(Codebase.class))).thenAnswer(invocation -> {
                    Codebase codebase = invocation.getArgument(0);
                    codebase.setId(1L);
                    return codebase;});

        Mockito.doThrow(new IllegalStateException("branch creation failed")).when(branchService)
                .createNewBranchForCodebase(any(Codebase.class), Mockito.eq(Branch.DEFAULT_NAME));

        assertThatThrownBy(() -> codebaseService.createCodebase(
                        "demo",
                        "Demo repository",
                        "http://localhost/git/demo.git",
                        owner.getKeycloakId())
        ).isInstanceOf(IllegalStateException.class).hasMessageContaining("branch creation failed");

        verify(storageService).deleteRepository("demo");
    }
}
