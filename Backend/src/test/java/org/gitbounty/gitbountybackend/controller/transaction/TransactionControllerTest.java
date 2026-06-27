package org.gitbounty.gitbountybackend.controller.transaction;

import org.gitbounty.gitbountybackend.controller.transaction.dto.CreateTransactionDto;
import org.gitbounty.gitbountybackend.exception.IssueNotFoundException;
import org.gitbounty.gitbountybackend.exception.TransactionNotFoundException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.controller.transaction.dto.DisputeTransactionDto;
import org.gitbounty.gitbountybackend.controller.transaction.dto.RejectTransactionDto;
import org.gitbounty.gitbountybackend.controller.transaction.dto.TransactionResponse;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.model.TransactionStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionController transactionController;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = buildTransaction(100L, TransactionStatus.PENDING);
    }

    @Nested
    class CreateTransactionTests {

        @Test
        void createTransaction_Success_ReturnsCreated() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            CreateTransactionDto request = new CreateTransactionDto(999L, 2L, 10L);
            when(transactionService.createEscrow(1L, 2L, 10L)).thenReturn(transaction);

            ResponseEntity<TransactionResponse> response = transactionController.createTransaction(request, jwt);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(100L, response.getBody().id());
            assertEquals("PENDING", response.getBody().status());
            verify(transactionService).createEscrow(1L, 2L, 10L);
        }

        @Test
        void createTransaction_ServiceThrowsIllegalArgument_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            CreateTransactionDto request = new CreateTransactionDto(1L, 2L, 10L);
            when(transactionService.createEscrow(1L, 2L, 10L)).thenThrow(new IllegalArgumentException("bad"));

            assertThrows(IllegalArgumentException.class, () ->
                transactionController.createTransaction(request, jwt));
        }

        @Test
        void createTransaction_ServiceThrowsIssueNotFound_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            CreateTransactionDto request = new CreateTransactionDto(1L, 2L, 10L);
            when(transactionService.createEscrow(1L, 2L, 10L)).thenThrow(new IssueNotFoundException(10L));

            assertThrows(IssueNotFoundException.class, () ->
                transactionController.createTransaction(request, jwt));
        }
    }

    @Nested
    class ApproveTransactionTests {

        @Test
        void approveTransaction_Success_ReturnsOk() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            Transaction approved = buildTransaction(100L, TransactionStatus.COMPLETED);
            when(transactionService.approveTransaction(100L, 1L)).thenReturn(approved);

            ResponseEntity<TransactionResponse> response = transactionController.approveTransaction(100L, jwt);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("COMPLETED", response.getBody().status());
            verify(transactionService).approveTransaction(100L, 1L);
        }

        @Test
        void approveTransaction_NotFound_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            when(transactionService.approveTransaction(100L, 1L))
                .thenThrow(new TransactionNotFoundException(100L));

            assertThrows(TransactionNotFoundException.class, () ->
                transactionController.approveTransaction(100L, jwt));
        }
    }

    @Nested
    class RejectTransactionTests {

        @Test
        void rejectTransaction_Success_ReturnsOk() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            RejectTransactionDto request = new RejectTransactionDto(100L, 1L, "Invalid work");
            Transaction rejected = buildTransaction(100L, TransactionStatus.REJECTED);
            when(transactionService.rejectTransaction(100L, 1L, "Invalid work")).thenReturn(rejected);

            ResponseEntity<TransactionResponse> response = transactionController.rejectTransaction(100L, request, jwt);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("REJECTED", response.getBody().status());
            verify(transactionService).rejectTransaction(100L, 1L, "Invalid work");
        }

        @Test
        void rejectTransaction_InvalidState_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            RejectTransactionDto request = new RejectTransactionDto(100L, 1L, "Reason");
            when(transactionService.rejectTransaction(100L, 1L, "Reason"))
                .thenThrow(new IllegalArgumentException("bad state"));

            assertThrows(IllegalArgumentException.class, () ->
                transactionController.rejectTransaction(100L, request, jwt));
        }

        @Test
        void rejectTransaction_NotFound_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            RejectTransactionDto request = new RejectTransactionDto(100L, 1L, "Reason");
            when(transactionService.rejectTransaction(100L, 1L, "Reason"))
                .thenThrow(new TransactionNotFoundException(100L));

            assertThrows(TransactionNotFoundException.class, () ->
                transactionController.rejectTransaction(100L, request, jwt));
        }

    }

    @Nested
    class DisputeTransactionTests {

        @Test
        void disputeTransaction_Success_ReturnsOk() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            DisputeTransactionDto request = new DisputeTransactionDto(100L, 1L, "Need review");
            Transaction disputed = buildTransaction(100L, TransactionStatus.DISPUTED);
            when(transactionService.disputeTransaction(100L, 1L, "Need review")).thenReturn(disputed);

            ResponseEntity<TransactionResponse> response = transactionController.disputeTransaction(100L, request, jwt);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("DISPUTED", response.getBody().status());
            verify(transactionService).disputeTransaction(100L, 1L, "Need review");
        }

        @Test
        void disputeTransaction_NotFound_Propagates() {
            Jwt jwt = authenticatedJwt(1L, "kc-1");
            DisputeTransactionDto request = new DisputeTransactionDto(100L, 1L, "Need review");
            when(transactionService.disputeTransaction(100L, 1L, "Need review"))
                .thenThrow(new TransactionNotFoundException(100L));

            assertThrows(TransactionNotFoundException.class, () ->
                transactionController.disputeTransaction(100L, request, jwt));
        }
    }

    @Nested
    class GetSingleTransactionTests {

        @Test
        void getTransaction_Found_ReturnsOk() {
            when(transactionService.getTransaction(100L)).thenReturn(Optional.of(transaction));

            ResponseEntity<TransactionResponse> response = transactionController.getTransaction(100L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(100L, response.getBody().id());
        }

        @Test
        void getTransaction_NotFound_ThrowsTransactionNotFoundException() {
            when(transactionService.getTransaction(100L)).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class, () ->
                transactionController.getTransaction(100L));
        }
    }

    @Nested
    class ListEndpointsTests {

        @Test
        void getTransactions_WithIssueId_UsesIssueQuery() {
            when(transactionService.getFilteredTransactions(null, null, 10L)).thenReturn(List.of(transaction));

            ResponseEntity<List<TransactionResponse>> response = transactionController.getTransactions(null, null, 10L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(transactionService).getFilteredTransactions(null, null, 10L);
        }

        @Test
        void getTransactions_WithUserIdAndStatus_FiltersByStatus() {
            Transaction completed = buildTransaction(101L, TransactionStatus.COMPLETED);
            when(transactionService.getFilteredTransactions("PENDING", 1L, null)).thenReturn(List.of(transaction, completed));

            ResponseEntity<List<TransactionResponse>> response = transactionController.getTransactions("PENDING", 1L, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());
            verify(transactionService).getFilteredTransactions("PENDING", 1L, null);
        }

        @Test
        void getTransactions_NoFilters_Success_ReturnsOk() {
            when(transactionService.getFilteredTransactions(null, null, null)).thenReturn(List.of(transaction));

            ResponseEntity<List<TransactionResponse>> response = transactionController.getTransactions(null, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(transactionService).getFilteredTransactions(null, null, null);
        }

        @Test
        void getUserTransactions_ServiceFailure_Propagates() {
            when(transactionService.getTransactionsForUser(1L)).thenThrow(new RuntimeException("boom"));

            assertThrows(RuntimeException.class, () ->
                transactionController.getUserTransactions(1L));
        }

        @Test
        void getTransactions_WithUserIdOnly_UsesUserQuery() {
            when(transactionService.getFilteredTransactions(null, 1L, null)).thenReturn(List.of(transaction));

            // Passing null for status and issueId
            ResponseEntity<List<TransactionResponse>> response = transactionController.getTransactions(null, 1L, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(transactionService).getFilteredTransactions(null, 1L, null);
        }

        @Test
        void getTransactions_NoParams_ReturnsPendingTransactions() {
            when(transactionService.getFilteredTransactions(null, null, null)).thenReturn(List.of(transaction));

            // Passing all null parameters to trigger the final else branch
            ResponseEntity<List<TransactionResponse>> response = transactionController.getTransactions(null, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(transactionService).getFilteredTransactions(null, null, null);
        }

        @Test
        void getTransactions_ServiceFailure_Propagates() {
            when(transactionService.getFilteredTransactions(null, null, null)).thenThrow(new RuntimeException("database error"));

            assertThrows(RuntimeException.class, () ->
                transactionController.getTransactions(null, null, null));
        }
    }

    @Nested
    class BalanceTests {

        @Test
        void getCreditBalance_Success_ReturnsOk() {
            when(transactionService.getUserCreditBalance(1L)).thenReturn(new BigDecimal("42.50"));

            var response = transactionController.getCreditBalance(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().userId());
            assertEquals(new BigDecimal("42.50"), response.getBody().creditBalance());
        }

        @Test
        void getCreditBalance_NotFound_Propagates() {
            when(transactionService.getUserCreditBalance(1L)).thenThrow(new UserNotFoundException("not found"));

            assertThrows(UserNotFoundException.class, () ->
                transactionController.getCreditBalance(1L));
        }
    }

    private Transaction buildTransaction(Long id, TransactionStatus status) {
        User from = new User();
        from.setId(1L);
        User to = new User();
        to.setId(2L);
        Issue issue = new Issue();
        issue.setId(10L);
        Bounty bounty = new Bounty();
        bounty.setId(1001L);
        bounty.setIssue(issue);

        return Transaction.builder()
            .id(id)
            .fromUser(from)
            .toUser(to)
            .bounty(bounty)
            .amount(new BigDecimal("20.00"))
            .status(status)
            .description("Bounty payout")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private Jwt authenticatedJwt(Long userId, String keycloakId) {
        User authenticatedUser = new User();
        authenticatedUser.setId(userId);
        authenticatedUser.setKeycloakId(keycloakId);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(keycloakId);
        when(userService.findByKeycloakId(keycloakId)).thenReturn(Optional.of(authenticatedUser));
        return jwt;
    }

    @Nested
    class SecurityEdgeCaseTests {

        @Test
        void extractUserIdFromJwt_MissingSubject_ThrowsIllegalStateException() {
            // Build a mock JWT that returns null or empty for the subject
            Jwt badJwt = mock(Jwt.class);
            when(badJwt.getSubject()).thenReturn("");

            // We call approveTransaction because it invokes extractUserIdFromJwt immediately
            assertThrows(IllegalStateException.class, () -> {
                transactionController.approveTransaction(100L, badJwt);
            });
        }
    }
}