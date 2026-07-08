package org.gitbounty.gitbountybackend.repository;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BountyRepository extends JpaRepository<Bounty, Long> {
    Optional<Bounty> findByIssueId(Long issueId);
    List<Bounty> findByStatus(BountyStatus status);
    List<Bounty> findByIssue_Repository_Id(Long repositoryId);
    List<Bounty> findByIssue_Repository_Owner_KeycloakId(String keycloakId);
    List<Bounty> findByIssue_AssignedTo_KeycloakId(String keycloakId);
}