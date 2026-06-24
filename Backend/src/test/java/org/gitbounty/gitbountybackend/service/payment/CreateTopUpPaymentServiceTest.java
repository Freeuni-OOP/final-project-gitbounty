package org.gitbounty.gitbountybackend.service.payment;

import org.gitbounty.gitbountybackend.exception.DuplicatePaymentRequestException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;
import org.gitbounty.gitbountybackend.model.CreditTopUpPaymentStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CreditTopUpPaymentRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditTopUpPaymentServiceTest {

    @Mock
    private CreditTopUpPaymentRepository paymentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CreditTopUpPaymentService paymentService;

    private User testUser;
    private CreateCreditTopUpCommand validCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setKeycloakId("user-123");
        testUser.setCreditBalance(new BigDecimal("500.00"));

        validCommand = new CreateCreditTopUpCommand(
                new BigDecimal("100"),
                "Jane Doe",
                "4242424242424242",
                12,
                2030,
                "123",
                "idempotency-key-1"
        );
    }

    @Test
    void createMockTopUp_ShouldSavePaymentAndAddCredits_WhenValidRequest() {
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-key-1")).thenReturn(Optional.empty());

        CreditTopUpPayment savedPayment = new CreditTopUpPayment();
        savedPayment.setId(10L);
        savedPayment.setUser(testUser);
        savedPayment.setCreditsGranted(new BigDecimal("100"));
        savedPayment.setAmountPaid(new BigDecimal("1000.00"));
        savedPayment.setStatus(CreditTopUpPaymentStatus.COMPLETED);

        when(paymentRepository.save(any(CreditTopUpPayment.class))).thenReturn(savedPayment);

        CreditTopUpPayment result = paymentService.createMockTopUp("user-123", validCommand);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(new BigDecimal("600.00"), testUser.getCreditBalance());
        verify(userService).save(testUser);
        verify(paymentRepository).save(any(CreditTopUpPayment.class));
    }

    @Test
    void createMockTopUp_ShouldThrowException_WhenUserNotFound() {
        when(userService.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            paymentService.createMockTopUp("unknown", validCommand);
        });

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createMockTopUp_ShouldThrowException_WhenCreditsExceed1000() {
        CreateCreditTopUpCommand invalidCommand = new CreateCreditTopUpCommand(
                new BigDecimal("1500"), "Jane Doe", "4242424242424242", 12, 2030, "123", "key-1"
        );
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.createMockTopUp("user-123", invalidCommand);
        });

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createMockTopUp_ShouldThrowException_WhenDuplicateIdempotencyKey() {
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-key-1"))
                .thenReturn(Optional.of(new CreditTopUpPayment()));

        assertThrows(DuplicatePaymentRequestException.class, () -> {
            paymentService.createMockTopUp("user-123", validCommand);
        });

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createMockTopUp_ShouldThrowException_WhenCreditsNotPositive() {
        CreateCreditTopUpCommand invalidCommand = new CreateCreditTopUpCommand(
                BigDecimal.ZERO, "Jane Doe", "4242424242424242", 12, 2030, "123", "key-1"
        );
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.createMockTopUp("user-123", invalidCommand);
        });
    }

    @Test
    void createMockTopUp_ShouldResolveCardBrands_ForAllValidTypes() {
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));
        // Echo the saved payment back to easily assert on its populated fields
        when(paymentRepository.save(any(CreditTopUpPayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Array format: {cardNumber, expectedBrand}
        String[][] cards = {
                {"51234567890123", "MASTERCARD"},
                {"3412345678901", "AMEX"},
                {"6011234567890", "DISCOVER"},
                {"9999999999999", "UNKNOWN"}
        };

        for (String[] card : cards) {
            CreateCreditTopUpCommand cmd = new CreateCreditTopUpCommand(
                    BigDecimal.TEN, "Jane Doe", card[0], 12, 2030, "123", "key-" + card[0]
            );
            CreditTopUpPayment payment = paymentService.createMockTopUp("user-123", cmd);
            assertEquals(card[1], payment.getCardBrand());
        }
    }

    @Test
    void validateRequest_ShouldThrowExceptions_ForInvalidInputs() {
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser));

        assertValidationThrows(null, "Payment request is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, null, "4242424242424242", 12, 2030, "123", "k"), "Cardholder name is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", null, 12, 2030, "123", "k"), "Card number is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 12, 2030, null, "k"), "CVV is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 12, 2030, "123", null), "Idempotency key is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 13, 2030, "123", "k"), "Expiry month must be between 1 and 12");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 12, null, "123", "k"), "Expiry year is required");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 12, 2000, "123", "k"), "Card has expired");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "424", 12, 2030, "123", "k"), "Card number must contain between 13 and 19 digits");
        assertValidationThrows(new CreateCreditTopUpCommand(BigDecimal.TEN, "Jane Doe", "4242424242424242", 12, 2030, "12", "k"), "CVV must be 3 or 4 digits");
    }

    // Helper method to keep line count low while asserting multiple validation exceptions
    private void assertValidationThrows(CreateCreditTopUpCommand command, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.createMockTopUp("user-123", command));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void getPaymentsForUser_ShouldReturnAllPaymentsForGivenUser() {
        // Create two mock payments
        CreditTopUpPayment payment1 = new CreditTopUpPayment();
        payment1.setId(10L);
        payment1.setUser(testUser);

        CreditTopUpPayment payment2 = new CreditTopUpPayment();
        payment2.setId(11L);
        payment2.setUser(testUser);

        List<CreditTopUpPayment> expectedPayments = List.of(payment1, payment2);

        // Mock the repository to return the list when the user's ID (1L) is queried
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(expectedPayments);

        List<CreditTopUpPayment> actualPayments = paymentService.getPaymentsForUser(1L);

        assertNotNull(actualPayments);
        assertEquals(2, actualPayments.size());
        assertTrue(actualPayments.contains(payment1));
        assertTrue(actualPayments.contains(payment2));
        verify(paymentRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

}