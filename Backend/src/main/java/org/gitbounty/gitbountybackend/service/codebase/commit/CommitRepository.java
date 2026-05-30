package org.gitbounty.gitbountybackend.service.codebase.commit;

import org.gitbounty.gitbountybackend.model.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitRepository extends JpaRepository<Commit, Long> {
    // Check if a commit hash already exists inside a given codebase
    boolean existsByCodebaseIdAndCommitHash(Long codebaseId, String commitHash);

    // Find a specific logged commit
    Optional<Commit> findByCodebaseIdAndCommitHash(Long codebaseId, String commitHash);
}