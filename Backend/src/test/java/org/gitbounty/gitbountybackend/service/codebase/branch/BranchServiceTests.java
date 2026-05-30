package org.gitbounty.gitbountybackend.service.codebase.branch;

import org.gitbounty.gitbountybackend.model.Branch;
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

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BranchServiceTests {

	@Mock
	private BranchRepository branchRepository;

	@InjectMocks
	private BranchService branchService;

	@Captor
	private ArgumentCaptor<Branch> branchCaptor;

	@BeforeEach
	void setUp() {
		// MockitoExtension handles initialization
	}

	@Test
	void createNewBranch_nullCodebase_throws() {
		assertThrows(EntityNotFoundException.class, () -> branchService.createNewBranchForCodebase(null, "main"));
	}

	@Test
	void createNewBranch_codebaseWithoutId_throws() {
		Codebase codebase = new Codebase(); // id is null
		assertThrows(EntityNotFoundException.class, () -> branchService.createNewBranchForCodebase(codebase, "main"));
	}

	@Test
	void createNewBranch_nullOrBlankName_throws() {
		Codebase codebase = new Codebase();
		codebase.setId(1L);

		assertThrows(IllegalArgumentException.class, () -> branchService.createNewBranchForCodebase(codebase, null));
		assertThrows(IllegalArgumentException.class, () -> branchService.createNewBranchForCodebase(codebase, "   "));
	}

	@Test
	void createNewBranch_normalizesShortName_andSaves() {
		Codebase codebase = new Codebase();
		codebase.setId(2L);

		when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
			Branch b = invocation.getArgument(0);
			b.setId(42L);
			return b;
		});

		Branch created = branchService.createNewBranchForCodebase(codebase, "main");

		assertNotNull(created);
		assertEquals(42L, created.getId());
		assertEquals("refs/heads/main", created.getName());
		assertSame(codebase, created.getCodebase());

		verify(branchRepository, times(1)).save(branchCaptor.capture());
		Branch saved = branchCaptor.getValue();
		assertEquals("refs/heads/main", saved.getName());
	}

	@Test
	void createNewBranch_allowsFullRefName() {
		Codebase codebase = new Codebase();
		codebase.setId(3L);

		when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Branch created = branchService.createNewBranchForCodebase(codebase, "refs/heads/feature-x");

		assertNotNull(created);
		assertEquals("refs/heads/feature-x", created.getName());
		assertSame(codebase, created.getCodebase());
		verify(branchRepository, times(1)).save(any(Branch.class));
	}

	@Test
	void updateBranch_setsLatestCommit_andSaves() {
		Codebase codebase = new Codebase();
		codebase.setId(10L);

		Branch existing = Branch.builder()
			.id(11L)
			.codebase(codebase)
			.name("refs/heads/main")
			.build();

		when(branchRepository.findByCodebaseIdAndName(eq(10L), eq("refs/heads/main")))
			.thenReturn(java.util.Optional.of(existing));

		when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

		Commit newCommit = Commit.builder().commitHash("abcd").build();
		Branch updated = branchService.updateBranchLatestCommit(codebase, "main", newCommit);

		assertNotNull(updated);
		assertSame(newCommit, updated.getLatestCommit());

		verify(branchRepository, times(1)).save(branchCaptor.capture());
		Branch saved = branchCaptor.getValue();
		assertSame(newCommit, saved.getLatestCommit());
	}

	@Test
	void deleteBranch_deletesFoundBranch() {
		Codebase codebase = new Codebase();
		codebase.setId(12L);

		Branch existing = Branch.builder()
			.id(13L)
			.codebase(codebase)
			.name("refs/heads/feature-delete")
			.build();

		when(branchRepository.findByCodebaseIdAndName(eq(12L), eq("refs/heads/feature-delete")))
			.thenReturn(java.util.Optional.of(existing));

		branchService.deleteBranchForCodebase(codebase, "feature-delete");

		verify(branchRepository, times(1)).delete(existing);
	}

	@Test
	void findBranch_returnsOptional_whenPresent_andNormalizesName() {
		Codebase codebase = new Codebase();
		codebase.setId(14L);

		Branch existing = Branch.builder()
			.id(15L)
			.codebase(codebase)
			.name("refs/heads/feature-find")
			.build();

		when(branchRepository.findByCodebaseIdAndName(eq(14L), eq("refs/heads/feature-find")))
			.thenReturn(java.util.Optional.of(existing));

		java.util.Optional<Branch> found = branchService.findBranchForCodebase(codebase, "feature-find");
		assertTrue(found.isPresent());
		assertEquals(existing, found.get());
	}

	@Test
	void listBranchesForCodebase_returnsAllBranches() {
		Codebase codebase = new Codebase();
		codebase.setId(20L);

		Branch b1 = Branch.builder().id(21L).codebase(codebase).name("refs/heads/a").build();
		Branch b2 = Branch.builder().id(22L).codebase(codebase).name("refs/heads/b").build();

		when(branchRepository.findByCodebaseId(eq(20L))).thenReturn(java.util.List.of(b1, b2));

		java.util.List<Branch> branches = branchService.listBranchesForCodebase(codebase);

		assertEquals(2, branches.size());
		assertTrue(branches.contains(b1));
		assertTrue(branches.contains(b2));
		verify(branchRepository, times(1)).findByCodebaseId(20L);
	}
}
