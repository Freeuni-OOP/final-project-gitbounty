package org.gitbounty.gitbountybackend.controller.transaction;

import jakarta.persistence.EntityNotFoundException;
import org.gitbounty.gitbountybackend.controller.transaction.dto.*;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    /**
     * Create an escrow transaction for a bounty completion.
     * POST /api/transactions
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionResponse> createTransaction(
        @RequestBody CreateTransactionDto request,
        @AuthenticationPrincipal Jwt jwt) {
        try {
            Long authenticatedUserId = extractUserIdFromJwt(jwt);

            Transaction transaction = transactionService.createEscrow(
                    authenticatedUserId,
                request.toUserId(),
                request.issueId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Approve a pending escrow transaction.
     * PATCH /api/transactions/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("@transactionPermissions.isCreator(#id, authentication.name)")
    public ResponseEntity<TransactionResponse> approveTransaction(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {
        try {
            Long approverUserId = extractUserIdFromJwt(jwt);

            Transaction transaction = transactionService.approveTransaction(id, approverUserId);
            return ResponseEntity.ok(TransactionResponse.from(transaction));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reject a pending escrow transaction.
     * PATCH /api/transactions/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("@transactionPermissions.isCreator(#id, authentication.name)")
    public ResponseEntity<TransactionResponse> rejectTransaction(
        @PathVariable Long id,
        @RequestBody RejectTransactionDto request,
        @AuthenticationPrincipal Jwt jwt) {
        try {
            Long rejecterUserId = extractUserIdFromJwt(jwt);

            Transaction transaction = transactionService.rejectTransaction(
                id,
                rejecterUserId,
                request.reason()
            );
            return ResponseEntity.ok(TransactionResponse.from(transaction));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Dispute a pending escrow transaction.
     * PATCH /api/transactions/{id}/dispute
     */
    @PatchMapping("/{id}/dispute")
    @PreAuthorize("@transactionPermissions.isInvolved(#id, authentication.name)")
    public ResponseEntity<TransactionResponse> disputeTransaction(
        @PathVariable Long id,
        @RequestBody DisputeTransactionDto request,
        @AuthenticationPrincipal Jwt jwt) {
        try {
            Long disputantUserId = extractUserIdFromJwt(jwt);


            Transaction transaction = transactionService.disputeTransaction(
                id,
                disputantUserId,
                request.reason()
            );
            return ResponseEntity.ok(TransactionResponse.from(transaction));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a transaction by ID.
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("@transactionPermissions.isInvolved(#id, authentication.name)")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        try {
            Transaction transaction = transactionService.getTransaction(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
            return ResponseEntity.ok(TransactionResponse.from(transaction));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all transactions with optional dynamic filters.
     * GET /api/transactions?status=PENDING&userId=1&issueId=5
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long issueId) {
        try {
            // Fetch transactions based on provided filters
            List<Transaction> transactions = transactionService.getFilteredTransactions(status, userId, issueId);

            List<TransactionResponse> responses = transactions.stream()
                    .map(TransactionResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's current credit balance.
     * GET /api/transactions/users/{userId}/balance
     */
    @GetMapping("/users/{userId}/balance")
    @PreAuthorize("@userPermissions.isOwner(#userId, authentication.name) or hasRole('admin')")
    public ResponseEntity<CreditBalanceResponse> getCreditBalance(@PathVariable Long userId) {
        try {
            var balance = transactionService.getUserCreditBalance(userId);
            return ResponseEntity.ok(new CreditBalanceResponse(userId, balance));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all transactions for a specific user (both sent and received).
     * GET /api/transactions/users/{userId}/transactions
     */
    @GetMapping("/users/{userId}/transactions")
    @PreAuthorize("@userPermissions.isOwner(#userId, authentication.name) or hasRole('admin')")
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsForUser(userId);
            List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Helper method to extract user ID from JWT token using keycloakId.
     * The JWT subject (sub) contains the keycloakId, which we look up in the database.
     */
    private Long extractUserIdFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        if (keycloakId == null || keycloakId.isEmpty()) {
            throw new IllegalStateException("JWT does not contain keycloak ID (sub)");
        }

        User user = userService.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException("User not found for keycloakId: " + keycloakId));

        return user.getId();
    }
}

