package org.gitbounty.gitbountybackend.service.codebase.issue;

import java.util.List;
import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByRepositoryName(String repositoryName);

    List<Issue> findByRepositoryId(Long repositoryId);

    Optional<Issue> findByRepositoryNameAndNumber(String repositoryName, Integer number);

    @Query("select max(i.number) from Issue i where i.repository.id = :repositoryId")
    Optional<Integer> findMaxNumberByRepositoryId(Long repositoryId);

    /**
     * Bulk-deletes the pull_request child rows for a repository's issues. Must run before
     * deleteIssuesByRepositoryId: pull_request.target_branch_id/source_branch_id and
     * pull_request.id (FK to issues.id) have no ON DELETE rule, so a PR row left behind
     * would block deleting either its parent issue or the branch it points to.
     *
     * Deliberately a native query, not JPQL: "delete from PullRequest" against a
     * JOINED-inheritance entity makes Hibernate plan a multi-table bulk delete backed by an
     * auto-created temp table (HT_issues). That works against a Hibernate-managed schema, but
     * this schema is Flyway-managed (ddl-auto=none) - Hibernate never creates the temp table,
     * and the delete fails with "Table 'HT_issues' doesn't exist" against real MySQL. Native
     * SQL bypasses that planner entirely and runs as the one statement below.
     */
    @Modifying
    @Query(value = "delete from pull_request where id in (select id from issues where repository_id = :repositoryId)",
            nativeQuery = true)
    int deletePullRequestsByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Bulk-deletes every issue for a repository. bounties.issue_id has ON DELETE CASCADE,
     * so this also removes their bounties at the database level.
     *
     * Native for the same reason as deletePullRequestsByRepositoryId above: Issue is the root
     * of the same JOINED hierarchy, so a JPQL bulk delete here hits the identical temp-table
     * requirement.
     */
    @Modifying
    @Query(value = "delete from issues where repository_id = :repositoryId", nativeQuery = true)
    int deleteIssuesByRepositoryId(@Param("repositoryId") Long repositoryId);
}