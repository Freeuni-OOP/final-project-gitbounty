package org.gitbounty.gitbountybackend.service.codebase.issue;

import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.User;

import org.gitbounty.gitbountybackend.model.IssueStatus;

import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
//import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import org.gitbounty.gitbountybackend.exception.IssueNotFoundException;

import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final CodebaseService codebaseService;
    private final UserService userService;

    public IssueService(
            IssueRepository issueRepository,
            CodebaseService codebaseService,
            UserService userService
    ) {
        this.issueRepository = issueRepository;
        this.codebaseService = codebaseService;
        this.userService = userService;
    }

    @Transactional
    public Issue createIssue(String repositoryName, String title, String description, Principal principal) {
        String normalizedTitle = normalizeTitle(title);
        User author = resolveAuthor(principal);
        Codebase codebase = codebaseService.getCodebase(repositoryName);

        // added this to get the number of the next issue in the current repository
        // (number of already existing issues) + 1
        Integer nextNumber = issueRepository.findMaxNumberByRepositoryId(codebase.getId())
                .map(maxNumber -> maxNumber + 1)
                .orElse(1);

        Issue issue = new Issue();
        issue.setNumber(nextNumber);
        issue.setTitle(normalizedTitle);
        issue.setDescription(normalizeDescription(description));
        issue.setAuthor(author);
        issue.setRepository(codebase);

        return issueRepository.saveAndFlush(issue);
    }

    private User resolveAuthor(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Issue title is required");
        }

        return title.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    @Transactional(readOnly = true)
    public List<Issue> listIssues(String repositoryName) {
        codebaseService.getCodebase(repositoryName);
        return issueRepository.findByRepositoryName(repositoryName);
    }

    @Transactional(readOnly = true)
    public Issue getIssue(String repositoryName, Integer issueNumber) {
        codebaseService.getCodebase(repositoryName);

        return issueRepository.findByRepositoryNameAndNumber(repositoryName, issueNumber)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found: #" + issueNumber));
    }

    @Transactional
    public Issue updateIssueState(String repositoryName, Integer issueNumber, IssueStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Issue status is required");
        }

        Issue issue = getIssue(repositoryName, issueNumber);
        issue.setStatus(status);

        return issueRepository.saveAndFlush(issue);
    }
}