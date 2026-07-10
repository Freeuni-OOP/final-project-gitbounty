package org.gitbounty.gitbountybackend.service.transaction;

import org.gitbounty.gitbountybackend.exception.IssueNotFoundException;
import org.gitbounty.gitbountybackend.exception.TransactionNotFoundException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.*;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public TransactionService(TransactionRepository transactionRepository,
                             UserRepository userRepository,
                             IssueRepository issueRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
    }
    /**
     * Create an escrow transaction for a bounty completion.
     * Creates a PENDING payout transaction for bounty funds that were
     * already placed into escrow when the bounty was created.
     *
     * @param fromUserId ID of the bounty creator (who posts the bounty)
     * @param toUserId ID of the bounty completer (who receives the credits)
     * @param issueId ID of the issue being completed
     * @return the created transaction
     * @throws UserNotFoundException if users not found, IssueNotFoundException if issue not found
     * @throws IllegalArgumentException if bounty amount is invalid or user lacks sufficient credits
     */
    @Transactional
    public Transaction createEscrow(Long fromUserId, Long toUserId, Long issueId) {
        // Validate users exist
        User fromUser = userRepository.findById(fromUserId)
            .orElseThrow(() -> new UserNotFoundException("Bounty creator (from_user) not found: " + fromUserId));

        User toUser = userRepository.findById(toUserId)
            .orElseThrow(() -> new UserNotFoundException("Bounty completer (to_user) not found: " + toUserId));

        // Validate issue exists and get bounty amount
        Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new IssueNotFoundException(issueId));

        if (issue.getBounty() == null || issue.getBounty().getAmount() == null) {
            throw new IllegalArgumentException("Issue has no bounty amount configured");
        }

        BigDecimal bountyAmount = BigDecimal.valueOf(issue.getBounty().getAmount());

        // Validate bounty amount
        if (bountyAmount == null || bountyAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid bounty amount: " + bountyAmount);
        }

        // Bounty was already funded when created.

        // Check if there's already a pending transaction for this issue
        Optional<Transaction> existingPending = transactionRepository.findByBountyIssueIdAndStatus(issueId, TransactionStatus.PENDING);
        if (existingPending.isPresent()) {
            throw new IllegalArgumentException("A pending transaction already exists for this issue");
        }

        // Create transaction in PENDING status
        Transaction transaction = Transaction.builder()
            .fromUser(fromUser)
            .toUser(toUser)
            .amount(bountyAmount)
            .status(TransactionStatus.PENDING)
            .bounty(issue.getBounty())
            .description("Bounty payout for issue #" + issue.getNumber() + ": " + issue.getTitle())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Approves a pending bounty payout and releases the already-escrowed
     * credits to the bounty completer.
     *
     * The bounty creator was charged when the bounty was created, so this
     * method will not deduct the amount from fromUser again.
     *
     * @param transactionId ID of the transaction to approve
     * @param approverUserId ID of the user approving (should be fromUser)
     * @return the updated transaction
     * @throws TransactionNotFoundException if transaction not found
     * @throws IllegalArgumentException if transaction is not PENDING or approver is not the creator
     */
    @Transactional
    public Transaction approveBountyPayout(Long transactionId, Long approverUserId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Verify transaction is in PENDING status
        if (!transaction.getStatus().equals(TransactionStatus.PENDING)) {
            throw new IllegalArgumentException("Can only approve PENDING transactions. Current status: " + transaction.getStatus());
        }

        // make sure it is bounty payout
        if (transaction.getBounty() == null || transaction.getToUser() == null) {
            throw new IllegalArgumentException("Only bounty payout transactions can be approved.");
        }

        User fromUser = transaction.getFromUser();

        // Verify approver is the bounty creator
        if (fromUser == null || !fromUser.getId().equals(approverUserId)) {
            throw new IllegalArgumentException("Only the bounty creator can approve this transaction");
        }

        // Update transaction status to COMPLETED
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setResolvedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Transfer credits: add to recipient's balance
        User toUser = transaction.getToUser();
        toUser.setCreditBalance(toUser.getCreditBalance().add(transaction.getAmount()));
        userRepository.save(toUser);

        // the deduction block was removed here, because they were already charged when the bounty was created

        // bounty creator already paid when the bounty was created
        return transactionRepository.save(transaction);
    }

    /**
     * Releases funds that are already held for a bounty.
     *
     * Existing pending payout is approved when present. Otherwise, the
     * funds (escrowed) are paid directly and recorded as a completed payout.
     */
    @Transactional
    public Transaction releaseBounty(Bounty bounty, User recipient) {
        if (bounty == null || bounty.getId() == null) {
            throw new IllegalArgumentException("A saved bounty is required.");
        }

        if (bounty.getAmount() == null || bounty.getAmount() <= 0) {
            throw new IllegalArgumentException("Bounty amount must be greater than zero.");
        }

        if (bounty.getStatus() != BountyStatus.OPEN && bounty.getStatus() != BountyStatus.ASSIGNED) {
            throw new IllegalArgumentException("Only an active bounty can be paid.");
        }

        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException("A saved bounty recipient is required.");
        }

        if (bounty.getIssue() == null
                || bounty.getIssue().getRepository() == null
                || bounty.getIssue().getRepository().getOwner() == null) {
            throw new IllegalArgumentException("The bounty owner could not be determined.");
        }

        User owner = bounty.getIssue().getRepository().getOwner();

        Optional<Transaction> pendingPayout = transactionRepository
                        .findByBountyIdAndStatus(bounty.getId(), TransactionStatus.PENDING);

        if (pendingPayout.isPresent()) {
            Transaction payout = pendingPayout.get();

            validatePayoutParticipants(payout, owner, recipient);

            return approveBountyPayout(payout.getId(), owner.getId());
        }

        BigDecimal amount = BigDecimal.valueOf(bounty.getAmount());

        BigDecimal currentBalance =
                recipient.getCreditBalance() == null ? BigDecimal.ZERO : recipient.getCreditBalance();

        recipient.setCreditBalance(currentBalance.add(amount));

        userRepository.save(recipient);

        LocalDateTime now = LocalDateTime.now();

        Transaction payout = Transaction.builder()
                .fromUser(owner)
                .toUser(recipient)
                .bounty(bounty)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description("Bounty payout: " + bounty.getTitle())
                .createdAt(now)
                .updatedAt(now)
                .resolvedAt(now)
                .build();

        return transactionRepository.save(payout);
    }

    /**
     * Make sure an existing pending payout belongs to the expected users.
     */
    private void validatePayoutParticipants(Transaction payout, User owner, User recipient) {
        if (payout.getFromUser() == null || !owner.getId().equals(payout.getFromUser().getId())) {
            throw new IllegalArgumentException("The pending payout has an unexpected bounty owner.");
        }

        if (payout.getToUser() == null || !recipient.getId().equals(payout.getToUser().getId())) {
            throw new IllegalArgumentException("The pending payout has an unexpected recipient.");
        }
    }

    /**
     * Reject a pending escrow transaction. No credits are transferred.
     * This can only be called by the bounty creator (fromUser).
     *
     * @param transactionId ID of the transaction to reject
     * @param rejecterUserId ID of the user rejecting (should be fromUser)
     * @param reason optional reason for rejection
     * @return the updated transaction
     * @throws TransactionNotFoundException if transaction not found
     * @throws IllegalArgumentException if transaction is not PENDING or rejecter is not the creator
     */
    @Transactional
    public Transaction rejectTransaction(Long transactionId, Long rejecterUserId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Verify transaction is in PENDING status
        if (!transaction.getStatus().equals(TransactionStatus.PENDING)) {
            throw new IllegalArgumentException("Can only reject PENDING transactions. Current status: " + transaction.getStatus());
        }

        // Verify rejecter is the bounty creator
        if (!transaction.getFromUser().getId().equals(rejecterUserId)) {
            throw new IllegalArgumentException("Only the bounty creator can reject this transaction");
        }

        // Update transaction status to REJECTED
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setResolvedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        if (reason != null && !reason.isEmpty()) {
            transaction.setDescription(transaction.getDescription() + " [Rejected: " + reason + "]");
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Mark a transaction as DISPUTED. This typically requires admin review.
     * Either party can dispute the transaction if they find an issue.
     *
     * @param transactionId ID of the transaction to dispute
     * @param disputantUserId ID of the user disputing
     * @param reason reason for the dispute
     * @return the updated transaction
     * @throws TransactionNotFoundException if transaction not found
     * @throws IllegalArgumentException if transaction is not PENDING
     */
    @Transactional
    public Transaction disputeTransaction(Long transactionId, Long disputantUserId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Must be pending to dispute
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Can only dispute PENDING transactions. Current status: " + transaction.getStatus());
        }

        // Transaction must be linked to a bounty
        if (transaction.getBounty() == null) {
            throw new IllegalStateException("Transaction is not linked to a bounty");
        }

        // Disputant must be creator or recipient
        Long fromUserId = transaction.getFromUser().getId();
        Long toUserId = transaction.getToUser().getId();
        if (!disputantUserId.equals(fromUserId) && !disputantUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Only the bounty creator or completer can dispute this transaction");
        }

        transaction.setStatus(TransactionStatus.DISPUTED);
        transaction.setResolvedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        if (reason != null && !reason.isBlank()) {
            String base = transaction.getDescription() == null ? "" : transaction.getDescription();
            transaction.setDescription(base + " [Disputed: " + reason.trim() + "]");
        }

        return transactionRepository.save(transaction);
    }
    /**
     * Get all pending transactions (in escrow)
     */
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus(TransactionStatus.PENDING);
    }

    /**
     * Get all transactions for a specific user (both sent and received)
     */
    public List<Transaction> getTransactionsForUser(Long userId) {
        List<Transaction> sent = transactionRepository.findByFromUserId(userId);
        List<Transaction> received = transactionRepository.findByToUserId(userId);

        sent.addAll(received);
        return sent;
    }

    /**
     * Get all transactions related to a specific issue
     */
    public List<Transaction> getTransactionsForIssue(Long issueId) {
        return transactionRepository.findByBountyIssueId(issueId);
    }

    /**
     * Get a specific transaction by ID
     */
    public Optional<Transaction> getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }

    /**
     * Get user's credit balance
     */
    public BigDecimal getUserCreditBalance(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return user.getCreditBalance();
    }

    /**
     * Manually adjust a user's credit balance (admin function)
     */
    @Transactional
    public void adjustUserCreditBalance(Long userId, BigDecimal amount, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        BigDecimal newBalance = user.getCreditBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative balance: " + newBalance);
        }

        user.setCreditBalance(newBalance);
        userRepository.save(user);
    }

    /**
     * Get transactions based on dynamic filters.
     * This encapsulates the business logic of which data set to pull.
     */
    public List<Transaction> getFilteredTransactions(String status, Long userId, Long issueId) {
        List<Transaction> transactions;

        // Determine base data source
        if (issueId != null) {
            transactions = getTransactionsForIssue(issueId);
        } else if (userId != null) {
            transactions = getTransactionsForUser(userId);
        } else {
            // Depending on what we decided earlier, this might be getPendingTransactions() or a new getAllTransactions()
            transactions = getPendingTransactions();
        }

        // Apply status filter if provided
        if (status != null && !status.isEmpty()) {
            transactions = transactions.stream()
                    .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        return transactions;
    }

    /**
     * Records the initial deduction of funds when a bounty is posted.
     * Creates a transaction where fromUser is the poster and toUser is null (meaning funds are held by the platform in system escrow).
     */
    @Transactional
    public Transaction recordBountyDeposit(
            User owner,
            Bounty bounty,
            BigDecimal amount,
            String description
    ) {
        if (owner == null) {
            throw new IllegalArgumentException("Bounty owner is required.");
        }

        if (bounty == null) {
            throw new IllegalArgumentException("Bounty is required.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Bounty deposit amount must be greater than zero."
            );
        }

        if (owner.getCreditBalance() == null
                || owner.getCreditBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient funds in wallet to put bounty into escrow."
            );
        }

        owner.setCreditBalance(
                owner.getCreditBalance().subtract(amount)
        );
        userRepository.save(owner);

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .fromUser(owner)
                .toUser(null)
                .bounty(bounty)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .resolvedAt(now)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Returns escrowed bounty funds to the owner and records the refund.
     */
    @Transactional
    public Transaction recordBountyRefund(
            User owner,
            Bounty bounty,
            BigDecimal amount,
            String description
    ) {
        if (owner == null) {
            throw new IllegalArgumentException("Bounty owner is required.");
        }

        if (bounty == null) {
            throw new IllegalArgumentException("Bounty is required.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bounty refund amount must be greater than zero.");
        }

        if (owner.getCreditBalance() == null) {
            throw new IllegalArgumentException("Bounty owner has no credit balance.");
        }

        owner.setCreditBalance(owner.getCreditBalance().add(amount));

        userRepository.save(owner);

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .fromUser(null)
                .toUser(owner)
                .bounty(bounty)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .resolvedAt(now)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Severs every pre-existing transaction's reference to a bounty belonging to this
     * repository, so those bounties (and their owning issues) can be deleted afterward
     * without violating the transactions.bounty_id foreign key. Bounties that are still
     * active at deletion time are refunded separately by BountyService.cancelIfActive,
     * called by CodebaseDeletionCascadeService right before each owning issue is actually
     * removed - this method only handles transactions that already existed before the
     * deletion started (deposits, prior payouts).
     *
     * Deliberately mutates the managed Transaction entities one at a time instead of
     * issuing a bulk UPDATE: a bulk query bypasses Hibernate's persistence context, so
     * any Transaction already loaded in this session would keep a stale in-memory
     * reference to a Bounty that a later cascade delete removes - which is exactly what
     * caused a TransientPropertyValueException at flush time. Mutating through the
     * session keeps Hibernate's own dirty-checking in sync with the database, so no
     * explicit flush or cache-clear is needed here.
     */
    @Transactional
    public void detachBountyReferencesForRepository(Long repositoryId) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("Repository id is required.");
        }

        for (Transaction transaction : transactionRepository.findByBounty_Issue_Repository_Id(repositoryId)) {
            transaction.setBounty(null);
        }
    }
}

