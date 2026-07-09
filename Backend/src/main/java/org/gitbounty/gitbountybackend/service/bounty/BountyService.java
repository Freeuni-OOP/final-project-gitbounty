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
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;

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
    private final IssueService issueService;

    @Autowired
    public BountyService(BountyRepository bountyRepository,
                         IssueRepository issueRepository,
                         UserRepository userRepository,
                         TransactionService transactionService,
                         IssueService issueService) {
        this.bountyRepository = bountyRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.issueService = issueService;
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

    /**
     * Pays the chosen recipient and marks the bounty as completed.
     */
    @Transactional
    public void completeBounty(Bounty bounty, Long recipientUserId) {
        if (bounty == null || bounty.getId() == null) {
            throw new IllegalArgumentException("A saved bounty is required.");
        }

        if (bounty.getStatus() == BountyStatus.COMPLETED) {
            throw new IllegalArgumentException("Bounty is already completed: " + bounty.getId());
        }

        if (bounty.getStatus() == BountyStatus.CANCELLED) {
            throw new IllegalArgumentException("A cancelled bounty cannot be paid: " + bounty.getId());
        }

        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new UserNotFoundException("Bounty recipient not found: " + recipientUserId));

        transactionService.releaseBounty(bounty, recipient);

        bounty.setStatus(BountyStatus.COMPLETED);
        bountyRepository.save(bounty);
    }

    /**
     * Finds and cancels a bounty by ID.
     */
    @Transactional
    public void cancelBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId)
                .orElseThrow(() -> new BountyNotFoundException(bountyId));
        cancelBounty(bounty);
    }

    /**
     * Refunds an active bounty and marks it as cancelled.
     */
    @Transactional
    public void cancelBounty(Bounty bounty) {
        if (bounty == null || bounty.getId() == null) {
            throw new IllegalArgumentException("A saved bounty is required.");
        }

        if (bounty.getStatus() == BountyStatus.COMPLETED) {
            throw new BountyAlreadyCompletedException(bounty.getId());
        }

        if (bounty.getStatus() == BountyStatus.CANCELLED) {
            throw new IllegalArgumentException("Bounty is already cancelled: " + bounty.getId());
        }

        User owner = userRepository.findByKeycloakId(bounty.getIssue().getRepository().getOwner().getKeycloakId())
                .orElseThrow(() -> new UserNotFoundException("Paying user not found"));

        BigDecimal refundAmount = BigDecimal.valueOf(bounty.getAmount());

        transactionService.recordBountyRefund(owner, bounty, refundAmount,
                "Bounty refund for issue #" + bounty.getIssue().getNumber() + ": " + bounty.getIssue().getTitle());

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
            Issue issue = bounty.getIssue();
            dto.setIssueId(issue.getId());

            if (issue.getAssignedTo() != null) {
                dto.setAssignedToId(issue.getAssignedTo().getId());
                dto.setAssignedToUsername(issue.getAssignedTo().getUsername());
            }
        }

        return dto;
    }

    public List<BountyDTO> getBountiesByRepository(Long repoId) {
        return bountyRepository.findByIssue_Repository_Id(repoId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BountyDTO> getRepositoryBountiesForUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        return bountyRepository.findByIssue_Repository_Owner_KeycloakId(keycloakId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BountyDTO> getClaimedBountiesForUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        return bountyRepository.findByIssue_AssignedTo_KeycloakId(keycloakId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BountyDTO claimBounty(Long bountyId, String claimantKeycloakId) {
        if (claimantKeycloakId == null || claimantKeycloakId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        Bounty bounty = bountyRepository.findById(bountyId)
                .orElseThrow(() -> new BountyNotFoundException(bountyId));

        if (bounty.getStatus() != BountyStatus.OPEN) {
            throw new IllegalArgumentException("Only open bounties can be claimed.");
        }

        Issue issue = bounty.getIssue();
        if (issue == null || issue.getId() == null) {
            throw new IllegalStateException("Bounty is not attached to a valid issue.");
        }

        User claimant = userRepository.findByKeycloakId(claimantKeycloakId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found."));

        User owner = issue.getRepository().getOwner();
        if (owner != null && claimantKeycloakId.equals(owner.getKeycloakId())) {
            throw new IllegalArgumentException("Repository owner cannot claim their own bounty.");
        }

        Issue claimedIssue = issueService.claimIssue(issue, claimant);

        bounty.setIssue(claimedIssue);
        bounty.setStatus(BountyStatus.ASSIGNED);

        return convertToDto(bountyRepository.save(bounty));
    }

    @Transactional
    public BountyDTO unclaimBounty(Long bountyId, String claimantKeycloakId) {
        if (claimantKeycloakId == null || claimantKeycloakId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }

        Bounty bounty = bountyRepository.findById(bountyId)
                .orElseThrow(() -> new BountyNotFoundException(bountyId));

        if (bounty.getStatus() != BountyStatus.ASSIGNED) {
            throw new IllegalArgumentException("Only assigned bounties can be left.");
        }

        Issue issue = bounty.getIssue();
        if (issue == null || issue.getId() == null) {
            throw new IllegalStateException("Bounty is not attached to a valid issue.");
        }

        User claimant = userRepository.findByKeycloakId(claimantKeycloakId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found."));

        Issue unclaimedIssue = issueService.unclaimIssue(issue, claimant);

        bounty.setIssue(unclaimedIssue);
        bounty.setStatus(BountyStatus.OPEN);

        return convertToDto(bountyRepository.save(bounty));
    }

    @Transactional
    public void cancelActiveBountiesForRepository(Long repositoryId) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("Repository id is required.");
        }

        List<Bounty> bounties = bountyRepository.findByIssue_Repository_Id(repositoryId);

        for (Bounty bounty : bounties) {
            if (bounty.getStatus() == BountyStatus.OPEN || bounty.getStatus() == BountyStatus.ASSIGNED) {
                cancelBounty(bounty);
            }
        }
    }
}