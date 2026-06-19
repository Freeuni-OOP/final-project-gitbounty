package org.gitbounty.gitbountybackend.service.codebase;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.gitbounty.gitbountybackend.service.codebase.git.GitService;
import org.gitbounty.gitbountybackend.service.codebase.storage.DockerVolumeCodebaseStorageService;
import org.junit.jupiter.api.Test;

class DockerVolumeCodebaseStorageServiceTests {

	@Test
	void createRepository_delegatesToGitService() {
		GitService mockGitService = mock(GitService.class);
		doNothing().when(mockGitService).createRepository("demo");

		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(mockGitService);

		storageService.createRepository("demo");

		verify(mockGitService).createRepository("demo");
	}

	@Test
	void deleteRepository_delegatesToGitService() {
		GitService mockGitService = mock(GitService.class);
		doNothing().when(mockGitService).deleteRepository("demo");

		DockerVolumeCodebaseStorageService storageService = new DockerVolumeCodebaseStorageService(mockGitService);

		storageService.deleteRepository("demo");

		verify(mockGitService).deleteRepository("demo");
	}
}
