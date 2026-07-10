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
    public Transaction cancelBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId)
                .orElseThrow(() -> new BountyNotFoundException(bountyId));
        return cancelBounty(bounty);
    }

    /**
     * Refunds an active bounty and marks it as cancelled. Returns the refund Transaction
     * so callers that are about to delete the bounty outright (see
     * BountyService.cancelIfActive, used by CodebaseDeletionCascadeService's repository-
     * deletion cascade) can sever that transaction's reference to it before the bounty row
     * disappears.
     */
    @Transactional
    public Transaction cancelBounty(Bounty bounty) {
        Transaction refund = refundEscrowedBounty(bounty);
        bounty.setStatus(BountyStatus.CANCELLED);
        bountyRepository.save(bounty);
        return refund;
    }

    /**
     * Refunds an active bounty's escrowed funds to its repository owner, without persisting
     * any change to the bounty entity itself - this is just the refund half of cancelBounty
     * above, which composes it with marking the bounty CANCELLED and saving it.
     *
     * Deliberately does NOT re-fetch the owner via userRepository: bounty.getIssue()
     * .getRepository().getOwner() is already the same, fully-loaded User (Codebase.owner is
     * eager) with zero extra queries.
     */
    @Transactional
    public Transaction refundEscrowedBounty(Bounty bounty) {
        if (bounty == null || bounty.getId() == null) {
            throw new IllegalArgumentException("A saved bounty is required.");
        }

        if (bounty.getStatus() == BountyStatus.COMPLETED) {
            throw new BountyAlreadyCompletedException(bounty.getId());
        }

        if (bounty.getStatus() == BountyStatus.CANCELLED) {
            throw new IllegalArgumentException("Bounty is already cancelled: " + bounty.getId());
        }

        User owner = bounty.getIssue().getRepository().getOwner();
        if (owner == null) {
            throw new UserNotFoundException("Paying user not found");
        }

        BigDecimal refundAmount = BigDecimal.valueOf(bounty.getAmount());

        return transactionService.recordBountyRefund(owner, bounty, refundAmount,
                "Bounty refund for issue #" + bounty.getIssue().getNumber() + ": " + bounty.getIssue().getTitle());
    }

    /**
     * Cancels and refunds a bounty if it's still active (OPEN or ASSIGNED); a no-op for any
     * other status, including a null bounty. Used by CodebaseService's repository-deletion
     * cascade immediately before the owning issue (and its cascaded bounty) is deleted, so
     * escrowed funds are never silently destroyed by that cascade.
     *
     * Severs the newly-created refund Transaction's bounty reference after cancelling: the
     * bounty row is about to be deleted by the caller right after this returns, and
     * transactions.bounty_id has no ON DELETE rule, so a refund transaction left pointing at
     * it would violate that foreign key once the delete actually executes.
     */
    @Transactional
    public void cancelIfActive(Bounty bounty) {
        if (bounty == null
                || (bounty.getStatus() != BountyStatus.OPEN && bounty.getStatus() != BountyStatus.ASSIGNED)) {
            return;
        }

        Transaction refund = cancelBounty(bounty);
        refund.setBounty(null);
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
            dto.setIssueNumber(issue.getNumber());
            dto.setIssueTitle(issue.getTitle());

            if (issue.getRepository() != null) {
                dto.setRepositoryName(issue.getRepository().getName());

                if (issue.getRepository().getOwner() != null) {
                    dto.setRepositoryOwnerUsername(
                            issue.getRepository().getOwner().getUsername()
                    );
                }
            }

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
}