package org.gitbounty.gitbountybackend.service.bounty;

import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.exception.*;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BountyService {

    private final BountyRepository bountyRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Autowired
    public BountyService(BountyRepository bountyRepository,
                         IssueRepository issueRepository,
                         UserRepository userRepository,
                         TransactionService transactionService) {
        this.bountyRepository = bountyRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public Bounty createBounty(BountyDTO dto, String userId) {
        // Validate Inputs
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new IllegalArgumentException("Bounty amount must be greater than zero.");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Bounty title is required.");
        }

        // Verify Issue and prevent duplicate bounties on the same issue
        Issue issue = issueRepository.findById(dto.getIssueId())
                .orElseThrow(() -> new IssueNotFoundException(dto.getIssueId()));

        if (issue.getStatus() != IssueStatus.OPEN) {
            throw new IllegalArgumentException("Bounties can only be created for open issues.");
        }

        if (bountyRepository.findByIssueId(dto.getIssueId()).isPresent()) {
            throw new IllegalArgumentException("This issue already has an active bounty.");
        }

        // Verify User and Credit Balance
        User owner = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with ID: " + userId));

        BigDecimal bountyAmount = BigDecimal.valueOf(dto.getAmount());
        if (owner.getCreditBalance() == null || owner.getCreditBalance().compareTo(bountyAmount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in wallet to put bounty into escrow.");
        }

        // Create Bounty Entity
        Bounty bounty = new Bounty();
        bounty.setTitle(dto.getTitle());
        bounty.setDescription(dto.getDescription());
        bounty.setAmount(dto.getAmount());
        bounty.setStatus(BountyStatus.OPEN);
        bounty.setIssue(issue);
        bounty.setCreatedAt(LocalDateTime.now());

        Bounty savedBounty = bountyRepository.save(bounty);

        transactionService.recordBountyDeposit(
                owner,
                savedBounty,
                bountyAmount,
                "Bounty deposit for issue #" + issue.getNumber() + ": " + issue.getTitle()
        );

        return savedBounty;
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

        if (bounty.getStatus() == BountyStatus.CANCELLED) {
            throw new IllegalArgumentException("Bounty is already cancelled: " + bountyId);
        }

        User owner = userRepository
                .findByKeycloakId(bounty.getIssue().getRepository().getOwner().getKeycloakId())
                .orElseThrow(() -> new UserNotFoundException("Paying user not found"));

        BigDecimal refundAmount = BigDecimal.valueOf(bounty.getAmount());

        transactionService.recordBountyRefund(
                owner,
                bounty,
                refundAmount,
                "Bounty refund for issue #" + bounty.getIssue().getNumber() + ": " + bounty.getIssue().getTitle()
        );

        bounty.setStatus(BountyStatus.CANCELLED);
        bountyRepository.save(bounty);
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
        dto.setDescription(bounty.getDescription());
        dto.setAmount(bounty.getAmount());
        dto.setStatus(bounty.getStatus());

        if (bounty.getIssue() != null) {
            dto.setIssueId(bounty.getIssue().getId());
        }

        return dto;
    }

    public List<BountyDTO> getBountiesByRepository(Long repoId) {
        return bountyRepository.findByIssue_Repository_Id(repoId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}