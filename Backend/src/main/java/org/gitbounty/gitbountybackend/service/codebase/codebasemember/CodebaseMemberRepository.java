package org.gitbounty.gitbountybackend.service.codebase.codebasemember;

import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface CodebaseMemberRepository extends JpaRepository<CodebaseMember, Long> {

    // Find a specific membership by repository and user IDs
    Optional<CodebaseMember> findByCodebaseIdAndUserId(Long codebaseId, Long userId);

    // Get all members belonging to a single repository
    List<CodebaseMember> findByCodebaseId(Long codebaseId);

    // Get all repositories a specific user belongs to
    List<CodebaseMember> findByUserId(Long userId);

    // Check if a user is already a member of a codebase
    boolean existsByCodebaseIdAndUserId(Long codebaseId, Long userId);

    // Count the collaborators on a codebase (useful for enforcing plan limits)
    long countByCodebaseId(Long codebaseId);

    // Find all members with a specific role inside a project (e.g., all MAINTAINERs)
    List<CodebaseMember> findByCodebaseIdAndRole(Long codebaseId, CodebaseRole role);
}