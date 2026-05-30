package org.gitbounty.gitbountybackend.service.codebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitbounty.gitbountybackend.service.codebase.storage.DockerVolumeCodebaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DockerVolumeCodebaseStorageServiceTests {

	@TempDir
	Path repositoriesRoot;

	@Test
	void createRepository_createsBareRepositoryOnDisk() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		storageService.createRepository("demo");

		Path repoPath = repositoriesRoot.resolve("demo.git");
		assertThat(Files.exists(repoPath)).isTrue();
		assertThat(Files.exists(repoPath.resolve("HEAD"))).isTrue();
	}

	@Test
	void createRepository_rejectsInvalidRepositoryPath() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		assertThatThrownBy(() -> storageService.createRepository("/etc/passwd"))
			.isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void createRepository_throwsConflictWhenRepositoryDirectoryExists() throws Exception {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);
		Files.createDirectories(repositoriesRoot.resolve("demo.git"));

		assertThatThrownBy(() -> storageService.createRepository("demo"))
			.isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void createRepository_wrapsIOExceptionFromCreateDirectories() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);
			mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("no permissions"));

			assertThatThrownBy(() -> storageService.createRepository("demo"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(error -> {
					ResponseStatusException ex = (ResponseStatusException) error;
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(ex.getCause()).isInstanceOf(IOException.class);
				});
		}
	}

	@Test
	void createRepository_wrapsGitApiException() throws Exception {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		InitCommand initCommand = mock(InitCommand.class);
		when(initCommand.setBare(true)).thenReturn(initCommand);
		when(initCommand.setDirectory(any(File.class))).thenReturn(initCommand);
		when(initCommand.call()).thenThrow(new GitAPIException("git failed") { });

		try (MockedStatic<Git> mockedGit = mockStatic(Git.class)) {
			mockedGit.when(Git::init).thenReturn(initCommand);

			assertThatThrownBy(() -> storageService.createRepository("demo"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
		}
	}

	@Test
	void createRepository_rethrowsRuntimeExceptionFromGitInit() throws Exception {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		InitCommand initCommand = mock(InitCommand.class);
		when(initCommand.setBare(true)).thenReturn(initCommand);
		when(initCommand.setDirectory(any(File.class))).thenReturn(initCommand);
		when(initCommand.call()).thenThrow(new RuntimeException("boom"));

		try (MockedStatic<Git> mockedGit = mockStatic(Git.class)) {
			mockedGit.when(Git::init).thenReturn(initCommand);

			assertThatThrownBy(() -> storageService.createRepository("demo"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("boom");
		}
	}

	@Test
	void deleteRepository_deletesExistingRepositoryDirectory() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);
		storageService.createRepository("demo");
		Path repoPath = repositoriesRoot.resolve("demo.git");
		assertThat(Files.exists(repoPath)).isTrue();

		storageService.deleteRepository("demo");

		assertThat(Files.exists(repoPath)).isFalse();
	}

	@Test
	void deleteRepository_noopWhenRepositoryDirectoryDoesNotExist() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		assertThatCode(() -> storageService.deleteRepository("missing")).doesNotThrowAnyException();
	}

	@Test
	void deleteRepository_rejectsInvalidRepositoryPath() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);

		assertThatThrownBy(() -> storageService.deleteRepository("/etc/passwd"))
			.isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void deleteRepository_wrapsIOExceptionFromWalk() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);
		Path repoPath = repositoriesRoot.resolve("demo.git");

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(repoPath)).thenReturn(true);
			// Exception thrown before any stream is created.
			@SuppressWarnings("resource")
			var throwingWalk = mockedFiles.when(() -> Files.walk(repoPath));
			throwingWalk.thenThrow(new IOException("walk failed"));

			assertThatThrownBy(() -> storageService.deleteRepository("demo"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(error -> {
					ResponseStatusException ex = (ResponseStatusException) error;
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
					assertThat(ex.getCause()).isInstanceOf(IOException.class);
				});
		}
	}

	@Test
	void deleteRepository_throwsIllegalStateWhenDeleteIfExistsFails() {
		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(repositoriesRoot);
		Path repoPath = repositoriesRoot.resolve("demo.git");

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(repoPath)).thenReturn(true);
			@SuppressWarnings("resource")
			var mockedWalk = mockedFiles.when(() -> Files.walk(repoPath));
			mockedWalk.thenReturn(Stream.of(repoPath));
			mockedFiles.when(() -> Files.deleteIfExists(repoPath)).thenThrow(new IOException("delete failed"));

			assertThatThrownBy(() -> storageService.deleteRepository("demo"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Unable to clean up repository directory");
		}
	}
}
