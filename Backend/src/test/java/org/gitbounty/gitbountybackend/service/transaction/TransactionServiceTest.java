package org.gitbounty.gitbountybackend.service.transaction;

import org.gitbounty.gitbountybackend.exception.IssueNotFoundException;
import org.gitbounty.gitbountybackend.exception.TransactionNotFoundException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.model.TransactionStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User fromUser;
    private User toUser;
    private Issue issue;
    private Bounty bounty;
    private Transaction pendingTransaction;

    @BeforeEach
    void setUp() {
        fromUser = new User();
        fromUser.setId(1L);
        fromUser.setCreditBalance(new BigDecimal("100.00"));

        toUser = new User();
        toUser.setId(2L);
        toUser.setCreditBalance(new BigDecimal("50.00"));

        bounty = new Bounty();
        bounty.setId(1001L);
        bounty.setAmount(20.00);

        issue = new Issue();
        issue.setId(10L);
        issue.setNumber(42);
        issue.setTitle("Fix Bug");
        issue.setBounty(bounty);

        // Build base transaction replacing .issue() with .bounty()
        pendingTransaction = Transaction.builder()
                .id(100L)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(new BigDecimal("20.00"))
                .status(TransactionStatus.PENDING)
                .bounty(bounty)
                .description("Bounty payout for issue #42: Fix Bug")
                .build();
    }

    @Nested
    class CreateEscrowTests {

        @Test
        void createEscrow_HappyPath_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
            when(transactionRepository.findByBountyIssueIdAndStatus(10L, TransactionStatus.PENDING)).thenReturn(Optional.empty());
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.createEscrow(1L, 2L, 10L);

            assertNotNull(result);
            assertEquals(TransactionStatus.PENDING, result.getStatus());
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void createEscrow_FromUserNotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_ToUserNotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_IssueNotFound_ThrowsIssueNotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(IssueNotFoundException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_NullBountyAmount_ThrowsIllegalArgumentException() {
            bounty.setAmount(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_NegativeBountyAmount_ThrowsIllegalArgumentException() {
            bounty.setAmount(-5.00);
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_InsufficientCredits_ThrowsIllegalArgumentException() {
            fromUser.setCreditBalance(new BigDecimal("10.00")); // Less than issue bounty (20.00)
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }

        @Test
        void createEscrow_ExistingPendingTransaction_ThrowsIllegalArgumentException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));
            when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
            when(transactionRepository.findByBountyIssueIdAndStatus(10L, TransactionStatus.PENDING))
                    .thenReturn(Optional.of(pendingTransaction));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.createEscrow(1L, 2L, 10L)
            );
        }
    }

    @Nested
    class ApproveTransactionTests {

        @Test
        void approveTransaction_HappyPath_Success() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.approveTransaction(100L, 1L);

            assertEquals(TransactionStatus.COMPLETED, result.getStatus());
            assertEquals(new BigDecimal("70.00"), toUser.getCreditBalance()); // 50 + 20
            assertEquals(new BigDecimal("80.00"), fromUser.getCreditBalance()); // 100 - 20
            assertNotNull(result.getResolvedAt());

            verify(userRepository).save(fromUser);
            verify(userRepository).save(toUser);
            verify(transactionRepository).save(pendingTransaction);
        }

        @Test
        void approveTransaction_NotFound_ThrowsTransactionNotFoundException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class, () ->
                    transactionService.approveTransaction(100L, 1L)
            );
        }

        @Test
        void approveTransaction_NotPendingStatus_ThrowsIllegalArgumentException() {
            pendingTransaction.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.approveTransaction(100L, 1L)
            );
        }

        @Test
        void approveTransaction_InvalidApprover_ThrowsIllegalArgumentException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            // Approver ID is 999L instead of fromUser ID (1L)
            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.approveTransaction(100L, 999L)
            );
        }
    }

    @Nested
    class RejectTransactionTests {

        @Test
        void rejectTransaction_WithReason_Success() {
            String initialDescription = pendingTransaction.getDescription();
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.rejectTransaction(100L, 1L, "Invalid work");

            assertEquals(TransactionStatus.REJECTED, result.getStatus());
            assertNotNull(result.getResolvedAt());
            assertEquals(initialDescription + " [Rejected: Invalid work]", result.getDescription());
            verify(transactionRepository).save(pendingTransaction);
        }

        @Test
        void rejectTransaction_EmptyReason_SuccessWithoutAppendingDescription() {
            String initialDescription = pendingTransaction.getDescription();
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.rejectTransaction(100L, 1L, "");

            assertEquals(TransactionStatus.REJECTED, result.getStatus());
            assertEquals(initialDescription, result.getDescription());
        }

        @Test
        void rejectTransaction_NotFound_ThrowsTransactionNotFoundException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class, () ->
                    transactionService.rejectTransaction(100L, 1L, "Reason")
            );
        }

        @Test
        void rejectTransaction_NotPendingStatus_ThrowsIllegalArgumentException() {
            pendingTransaction.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.rejectTransaction(100L, 1L, "Reason")
            );
        }

        @Test
        void rejectTransaction_InvalidRejecter_ThrowsIllegalArgumentException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.rejectTransaction(100L, 999L, "Reason")
            );
        }
    }

    @Nested
    class DisputeTransactionTests {

        @Test
        void disputeTransaction_ByFromUser_Success() {
            String initialDescription = pendingTransaction.getDescription();
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.disputeTransaction(100L, 1L, "Code missing features");

            assertEquals(TransactionStatus.DISPUTED, result.getStatus());
            assertEquals(initialDescription + " [Disputed: Code missing features]", result.getDescription());
        }

        @Test
        void disputeTransaction_ByToUser_SuccessNullReason() {
            String initialDescription = pendingTransaction.getDescription();
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

            Transaction result = transactionService.disputeTransaction(100L, 2L, null);

            assertEquals(TransactionStatus.DISPUTED, result.getStatus());
            assertEquals(initialDescription, result.getDescription());
        }

        @Test
        void disputeTransaction_NotFound_ThrowsTransactionNotFoundException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class, () ->
                    transactionService.disputeTransaction(100L, 1L, "Reason")
            );
        }

        @Test
        void disputeTransaction_NotPendingStatus_ThrowsIllegalArgumentException() {
            pendingTransaction.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.disputeTransaction(100L, 1L, "Reason")
            );
        }

        @Test
        void disputeTransaction_UnauthorizedUser_ThrowsIllegalArgumentException() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            // User 999L is neither creator (1L) nor recipient (2L)
            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.disputeTransaction(100L, 999L, "Reason")
            );
        }
    }

    @Nested
    class QueriesAndLookupsTests {

        @Test
        void getPendingTransactionsTest() {
            List<Transaction> list = List.of(pendingTransaction);
            when(transactionRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(list);

            List<Transaction> result = transactionService.getPendingTransactions();

            assertEquals(1, result.size());
            verify(transactionRepository).findByStatus(TransactionStatus.PENDING);
        }

        @Test
        void getTransactionsForUserTest() {
            // Must return an arraylist that supports modifications (.addAll)
            List<Transaction> sent = new ArrayList<>(List.of(pendingTransaction));
            List<Transaction> received = new ArrayList<>(List.of(pendingTransaction));

            when(transactionRepository.findByFromUserId(1L)).thenReturn(sent);
            when(transactionRepository.findByToUserId(1L)).thenReturn(received);

            List<Transaction> result = transactionService.getTransactionsForUser(1L);

            assertEquals(2, result.size());
            verify(transactionRepository).findByFromUserId(1L);
            verify(transactionRepository).findByToUserId(1L);
        }

        @Test
        void getTransactionsForIssueTest() {
            List<Transaction> list = List.of(pendingTransaction);
            when(transactionRepository.findByBountyIssueId(10L)).thenReturn(list);

            List<Transaction> result = transactionService.getTransactionsForIssue(10L);

            assertEquals(1, result.size());
            verify(transactionRepository).findByBountyIssueId(10L);
        }

        @Test
        void getTransactionTest() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(pendingTransaction));

            Optional<Transaction> result = transactionService.getTransaction(100L);

            assertTrue(result.isPresent());
            assertEquals(100L, result.get().getId());
        }

        @Test
        void getFilteredTransactionsTest() {
            List<Transaction> list = List.of(pendingTransaction);
            when(transactionRepository.findByBountyIssueId(10L)).thenReturn(list);

            List<Transaction> result = transactionService.getFilteredTransactions("PENDING", null, 10L);

            assertEquals(1, result.size());
            verify(transactionRepository).findByBountyIssueId(10L);
        }

        @Test
        void getUserCreditBalance_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));

            BigDecimal balance = transactionService.getUserCreditBalance(1L);

            assertEquals(new BigDecimal("100.00"), balance);
        }

        @Test
        void getUserCreditBalance_NotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () ->
                    transactionService.getUserCreditBalance(1L)
            );
        }
    }

    @Nested
    class AdjustUserCreditBalanceTests {

        @Test
        void adjustUserCreditBalance_PositiveAdjustment_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));

            transactionService.adjustUserCreditBalance(1L, new BigDecimal("50.00"), "Bonus");

            assertEquals(new BigDecimal("150.00"), fromUser.getCreditBalance());
            verify(userRepository).save(fromUser);
        }

        @Test
        void adjustUserCreditBalance_NegativeAdjustment_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));

            transactionService.adjustUserCreditBalance(1L, new BigDecimal("-40.00"), "Correction");

            assertEquals(new BigDecimal("60.00"), fromUser.getCreditBalance());
            verify(userRepository).save(fromUser);
        }

        @Test
        void adjustUserCreditBalance_UserNotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () ->
                    transactionService.adjustUserCreditBalance(1L, new BigDecimal("10.00"), "Reason")
            );
        }

        @Test
        void adjustUserCreditBalance_ResultingNegativeBalance_ThrowsIllegalArgumentException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));

            // Subtracting 150 from 100 goes into negative
            assertThrows(IllegalArgumentException.class, () ->
                    transactionService.adjustUserCreditBalance(1L, new BigDecimal("-150.00"), "Over-deduction")
            );
        }
    }
}