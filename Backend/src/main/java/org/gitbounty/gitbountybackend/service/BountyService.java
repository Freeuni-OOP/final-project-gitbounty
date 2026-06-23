package org.gitbounty.gitbountybackend.service;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.User.UserRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BountyService {

    private final BountyRepository bountyRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    @Autowired
    public BountyService(BountyRepository bountyRepository, IssueRepository issueRepository, UserRepository userRepository) {
        this.bountyRepository = bountyRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Bounty createBounty(BountyDTO dto, String userId) {
        Issue issue = issueRepository.findById(dto.getIssueId()).orElseThrow(() -> new IssueNotFoundException(dto.getIssueId()));
        User owner = userRepository.findByKeycloakId(userId).orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database with ID: " + userId));

        java.math.BigDecimal bountyAmount = java.math.BigDecimal.valueOf(dto.getAmount());
        if (owner.getCreditBalance() == null || owner.getCreditBalance().compareTo(bountyAmount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in wallet to put bounty into escrow.");
        }

        owner.setCreditBalance(owner.getCreditBalance().subtract(bountyAmount));
        userRepository.save(owner);

        Bounty bounty = new Bounty();
        bounty.setTitle(dto.getTitle());
        bounty.setDescription(dto.getDescription());
        bounty.setAmount(dto.getAmount());
        bounty.setStatus(BountyStatus.OPEN);
        bounty.setIssue(issue);
        bounty.setCreatedAt(java.time.LocalDateTime.now());

        return bountyRepository.save(bounty);
    }

    @Transactional
    public void completeBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId).orElseThrow(() -> new BountyNotFoundException(bountyId));

        bounty.setStatus(BountyStatus.COMPLETED);
        bountyRepository.save(bounty);

        if (bounty.getIssue() != null) {
            Issue issue = bounty.getIssue();
            issue.setStatus(IssueStatus.CLOSED);
            issueRepository.save(issue);
        }
    }

    @Transactional
    public void closeIssueAndBounty(Long issueId) {
        Issue issue = issueRepository.findById(issueId).orElseThrow(() -> new IssueNotFoundException(issueId));

        issue.setStatus(IssueStatus.CLOSED);
        issueRepository.save(issue);

        bountyRepository.findByIssueId(issueId).ifPresent(bounty -> {
            bounty.setStatus(BountyStatus.COMPLETED);
            bountyRepository.save(bounty);
        });
    }

    @Transactional
    public void cancelBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId).orElseThrow(() -> new BountyNotFoundException(bountyId));

        if (bounty.getStatus() == BountyStatus.COMPLETED) {
            throw new BountyAlreadyCompletedException(bountyId);
        }

        User owner = userRepository.findByKeycloakId(bounty.getIssue().getRepository().getOwner().getKeycloakId()).orElseThrow(() -> new UserNotFoundException("Paying user not found"));

        owner.setCreditBalance(owner.getCreditBalance().add(java.math.BigDecimal.valueOf(bounty.getAmount())));
        userRepository.save(owner);

        bounty.setStatus(BountyStatus.CANCELLED);
        bountyRepository.save(bounty);

        if (bounty.getIssue() != null) {
            Issue issue = bounty.getIssue();
            issue.setStatus(IssueStatus.OPEN);
            issueRepository.save(issue);
        }
    }

    public List<BountyDTO> getAllBounties() {
        return bountyRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<BountyDTO> getBountiesByStatus(BountyStatus status) {
        return bountyRepository.findByStatus(status).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public BountyDTO getBountyById(Long id) {
        Bounty bounty = bountyRepository.findById(id).orElseThrow(() -> new BountyNotFoundException(id));
        return convertToDto(bounty);
    }

    private BountyDTO convertToDto(Bounty bounty) {
        BountyDTO dto = new BountyDTO();
        dto.setId(bounty.getId());
        dto.setTitle(bounty.getTitle());
        dto.setAmount(bounty.getAmount());
        dto.setStatus(bounty.getStatus());
        if (bounty.getIssue() != null) dto.setIssueId(bounty.getIssue().getId());
        return dto;
    }
}