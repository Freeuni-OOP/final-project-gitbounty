package org.gitbounty.gitbountybackend.service.codebase.branch;

import jakarta.persistence.EntityNotFoundException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Commit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BranchService {

	private final BranchRepository branchRepository;

	public BranchService(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

    @Transactional
    public Branch createNewBranchForCodebase(Codebase codebase, String branchName, Commit latestCommit) {
		assertCodebasePersisted(codebase);
		assertBranchNameValid(branchName);

		// Normalize to full ref name used in the DB (store as "refs/heads/..." )
		final String normalized = branchName.startsWith("refs/heads/") ? branchName : "refs/heads/" + branchName;

		// Build branch entity and attach to codebase (keeps bidirectional association)
		Branch toCreate = Branch.builder()
				.codebase(codebase)
				.name(normalized)
				.latestCommit(latestCommit)
				.build();

		return branchRepository.save(toCreate);
	}

	@Transactional
	public Branch createNewBranchForCodebase(Codebase codebase, String branchName) {
		return createNewBranchForCodebase(codebase, branchName, null);
	}

	@Transactional
	public Branch updateBranchLatestCommit(Codebase codebase, String branchName, Commit latestCommit) {
		Branch branch = findBranchForCodebase(codebase, branchName)
			.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchName));

		branch.setLatestCommit(latestCommit);
		return branchRepository.save(branch);
	}

	@Transactional
	public void deleteBranchForCodebase(Codebase codebase, String branchName) {
		Branch branch = findBranchForCodebase(codebase, branchName)
			.orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchName));

		branchRepository.delete(branch);
	}

	@Transactional(readOnly = true)
	public Optional<Branch> findBranchForCodebase(Codebase codebase, String branchName) {
		assertCodebasePersisted(codebase);
		assertBranchNameValid(branchName);

		final String normalized = branchName.startsWith("refs/heads/") ? branchName : "refs/heads/" + branchName;
		return branchRepository.findByCodebaseIdAndName(codebase.getId(), normalized);
	}

	private void assertCodebasePersisted(Codebase codebase) {
		if (codebase == null || codebase.getId() == null) {
			throw new EntityNotFoundException("Codebase must be a persisted entity");
		}
	}
	private void assertBranchNameValid(String branchName) {
		if (branchName == null || branchName.isBlank()) {
			throw new IllegalArgumentException("Branch name is required");
		}
	}
}
