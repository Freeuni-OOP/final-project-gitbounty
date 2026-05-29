package org.gitbounty.gitbountybackend.service.codebase.branch;

import jakarta.persistence.EntityNotFoundException;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

	private final BranchRepository branchRepository;

	public BranchService(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

    @Transactional
    public Branch createNewBranchForCodebase(Codebase codebase, String branchName) {
		if (codebase == null || codebase.getId() == null) {
			throw new EntityNotFoundException("Codebase must be a persisted entity");
		}

		if (branchName == null || branchName.isBlank()) {
			throw new IllegalArgumentException("Branch name is required");
		}

		// Normalize to full ref name used in the DB (store as "refs/heads/..." )
		final String normalized = branchName.startsWith("refs/heads/") ? branchName : "refs/heads/" + branchName;

		// Build branch entity and attach to codebase (keeps bidirectional association)
		Branch toCreate = Branch.builder()
				.codebase(codebase)
				.name(normalized)
				.latestCommit(null)
				.build();

		return branchRepository.save(toCreate);
	}
}
