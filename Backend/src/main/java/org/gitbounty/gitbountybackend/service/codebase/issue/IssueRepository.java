package org.gitbounty.gitbountybackend.service.codebase.issue;

import org.gitbounty.gitbountybackend.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IssueRepository extends JpaRepository<Issue, Long> {
	List<Issue> findByRepositoryId(Long repositoryId);
}
