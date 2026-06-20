package org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest;

import org.gitbounty.gitbountybackend.exception.ResourceNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PullRequestPersistenceService {
    private final PullRequestRepository repository;

    PullRequestPersistenceService(PullRequestRepository repository) {
        this.repository = repository;
    }

    // Accepting the DTO and resolved domain objects
    public PullRequest create(CreatePullRequestCommand command, User author, Codebase codebase,
                              Branch source, Branch target, Integer number) {
        PullRequest pr = new PullRequest();
        pr.setTitle(PullRequest.normalizeTitle(command.title()));
        pr.setDescription(PullRequest.normalizeDescription(command.description()));
        pr.setNumber(number);
        pr.setAuthor(author);
        pr.setRepository(codebase);
        pr.setSourceBranch(source);
        pr.setTargetBranch(target);

        return repository.saveAndFlush(pr);
    }

    public PullRequest finalizeMerge(Long prId) {
        PullRequest pr = repository.findById(prId)
            .orElseThrow(() -> new ResourceNotFoundException("PR not found with ID: " + prId));

        pr.setStatus(IssueStatus.CLOSED);
        pr.setMergedAt(Instant.now());
        return repository.save(pr);
    }

    public void delete(Long prId) {
        repository.deleteById(prId);
    }

    public void updatePRStatus(Long id, IssueStatus issueStatus) {
        var pr = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PR not found with ID: " + id));
        pr.setStatus(issueStatus);
    }
}