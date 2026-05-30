package org.gitbounty.gitbountybackend.service.codebase.commit;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommitServiceTests {

	@Mock
	private CommitRepository commitRepository;

	@InjectMocks
	private CommitService commitService;

	@Captor
	private ArgumentCaptor<Commit> commitCaptor;

	@BeforeEach
	void setUp() {
		// MockitoExtension handles initialization
	}

	@Test
	void findByCodebaseIdAndCommitHash_delegatesToRepository() {
		Commit c = Commit.builder().id(1L).commitHash("abcd").build();
		when(commitRepository.findByCodebaseIdAndCommitHash(5L, "abcd")).thenReturn(Optional.of(c));

		Optional<Commit> result = commitService.findByCodebaseIdAndCommitHash(5L, "abcd");

		assertTrue(result.isPresent());
		assertEquals(c, result.get());
	}

	@Test
	void persistCommitIfMissing_nullCodebase_throws() {
		assertThrows(IllegalArgumentException.class, () ->
			commitService.persistCommitIfMissing(null, "h", "a", "e", "m", Instant.now())
		);

		Codebase cb = new Codebase(); // id null
		assertThrows(IllegalArgumentException.class, () ->
			commitService.persistCommitIfMissing(cb, "h", "a", "e", "m", Instant.now())
		);
	}

	@Test
	void persistCommitIfMissing_returnsExisting_whenPresent() {
		Codebase cb = new Codebase(); cb.setId(7L);
		Commit existing = Commit.builder().id(9L).commitHash("deadbeef").build();
		when(commitRepository.findByCodebaseIdAndCommitHash(7L, "deadbeef")).thenReturn(Optional.of(existing));

		Commit result = commitService.persistCommitIfMissing(cb, "deadbeef", "Name", "email@example.com", "msg", Instant.ofEpochSecond(1000));

		assertSame(existing, result);
		verify(commitRepository, never()).save(any());
	}

	@Test
	void persistCommitIfMissing_savesNewCommit_whenMissing() {
		Codebase cb = new Codebase(); cb.setId(11L);
		when(commitRepository.findByCodebaseIdAndCommitHash(11L, "cafebabefcafebabefcafebabefcafebabefcafeb"))
			.thenReturn(Optional.empty());

		Commit saved = Commit.builder().id(42L).commitHash("cafebabefcafebabefcafebabefcafebabefcafeb").build();
		when(commitRepository.save(any(Commit.class))).thenReturn(saved);

		Instant when = Instant.now();
		Commit result = commitService.persistCommitIfMissing(cb, saved.getCommitHash(), "Author", "a@b.com", "hello", when);

		assertNotNull(result);
		assertEquals(42L, result.getId());

		verify(commitRepository, times(1)).save(commitCaptor.capture());
		Commit toSave = commitCaptor.getValue();
		assertEquals(cb, toSave.getCodebase());
		assertEquals(saved.getCommitHash(), toSave.getCommitHash());
		assertEquals("Author", toSave.getAuthorName());
		assertEquals("a@b.com", toSave.getAuthorEmail());
		assertEquals("hello", toSave.getMessage());
		assertEquals(when, toSave.getCommittedAt());
	}
}


