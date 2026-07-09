package org.gitbounty.gitbountybackend.service.codebase.issue;

import java.util.List;
import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByRepositoryName(String repositoryName);

    // Includes PullRequest rows too - querying the JOINED-inheritance root returns every
    // subtype. CodebaseService relies on this: it deletes PullRequests via
    // PullRequestRepository first, so only plain issues are left here by the time it runs.
    List<Issue> findByRepositoryId(Long repositoryId);

    Optional<Issue> findByRepositoryNameAndNumber(String repositoryName, Integer number);

    @Query("select max(i.number) from Issue i where i.repository.id = :repositoryId")
    Optional<Integer> findMaxNumberByRepositoryId(Long repositoryId);
}