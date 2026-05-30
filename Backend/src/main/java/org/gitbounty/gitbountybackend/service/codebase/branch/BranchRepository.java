package org.gitbounty.gitbountybackend.service.codebase.branch;

import org.gitbounty.gitbountybackend.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    // Find a branch by name inside a codebase (e.g., "refs/heads/main")
    Optional<Branch> findByCodebaseIdAndName(Long codebaseId, String name);

    // Get all branches belonging to a specific codebase
    List<Branch> findByCodebaseId(Long codebaseId);

    // Delete a branch when a user runs `git push origin :branch_name`
    void deleteByCodebaseIdAndName(Long codebaseId, String name);
}