package org.gitbounty.gitbountybackend.service.codebase.issue;

import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IssueService {

	private final IssueRepository issueRepository;

	IssueService(IssueRepository issueRepository) {
		this.issueRepository = issueRepository;
	}

	@Transactional
	public Issue createIssue(Issue issue) {
		validateIssue(issue);

		if (issue.getStatus() == null) {
			issue.setStatus(IssueStatus.OPEN);
		}

		return issueRepository.save(issue);
	}

	@Transactional
	public Issue updateIssue(Issue issue) {
		validateIssue(issue);

		if (issue.getId() == null) {
			throw new IllegalArgumentException("Issue id is required for update");
		}

		return issueRepository.save(issue);
	}

	@Transactional(readOnly = true)
	public Optional<Issue> findIssueById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Issue id is required");
		}

		return issueRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public List<Issue> getIssuesForRepository(Long repositoryId) {
		if (repositoryId == null) {
			throw new IllegalArgumentException("Repository id is required");
		}

		return issueRepository.findByRepositoryId(repositoryId);
	}

	@Transactional
	public void deleteIssueById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Issue id is required");
		}

		issueRepository.deleteById(id);
	}

	private void validateIssue(Issue issue) {
		if (issue == null) {
			throw new IllegalArgumentException("Issue is required");
		}

		if (issue.getTitle() == null || issue.getTitle().isBlank()) {
			throw new IllegalArgumentException("Issue title is required");
		}

		if (issue.getAuthor() == null) {
			throw new IllegalArgumentException("Issue author is required");
		}

		if (issue.getRepository() == null) {
			throw new IllegalArgumentException("Issue repository is required");
		}
	}

}
