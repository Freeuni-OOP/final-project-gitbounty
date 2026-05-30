package org.gitbounty.gitbountybackend.service.codebase.commit;

import java.time.Instant;
import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Commit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommitService {

	private final CommitRepository commitRepository;

	public CommitService(CommitRepository commitRepository) {
		this.commitRepository = commitRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Commit> findByCodebaseIdAndCommitHash(Long codebaseId, String commitHash) {
		return commitRepository.findByCodebaseIdAndCommitHash(codebaseId, commitHash);
	}

	@Transactional
	public Commit persistCommitIfMissing(
		Codebase codebase,
		String commitHash,
		String authorName,
		String authorEmail,
		String message,
		Instant committedAt
	) {
		if (codebase == null || codebase.getId() == null) {
			throw new IllegalArgumentException("Codebase must be a persisted entity");
		}

		return commitRepository.findByCodebaseIdAndCommitHash(codebase.getId(), commitHash)
			.orElseGet(() -> commitRepository.save(Commit.builder()
				.codebase(codebase)
				.commitHash(commitHash)
				.authorName(authorName)
				.authorEmail(authorEmail)
				.message(message)
				.committedAt(committedAt)
				.build()));
	}
}
