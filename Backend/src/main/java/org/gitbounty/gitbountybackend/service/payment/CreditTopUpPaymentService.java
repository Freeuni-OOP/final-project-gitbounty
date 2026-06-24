package org.gitbounty.gitbountybackend.service.payment;

import org.gitbounty.gitbountybackend.exception.DuplicatePaymentRequestException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;
import org.gitbounty.gitbountybackend.model.CreditTopUpPaymentStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CreditTopUpPaymentRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CreditTopUpPaymentService {

    private static final Logger log = LoggerFactory.getLogger(CreditTopUpPaymentService.class);
    private static final BigDecimal CREDIT_PRICE_USD = BigDecimal.TEN;
    private static final BigDecimal MAX_CREDITS_PER_TRANSACTION = new BigDecimal("1000");

    private final CreditTopUpPaymentRepository paymentRepository;
    private final UserService userService;

    public CreditTopUpPaymentService(CreditTopUpPaymentRepository paymentRepository, UserService userService) {
        this.paymentRepository = paymentRepository;
        this.userService = userService;
    }

    @Transactional
    public CreditTopUpPayment createMockTopUp(String keycloakId, CreateCreditTopUpCommand request) {
        User user = userService.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new UserNotFoundException("User not found for keycloakId: " + keycloakId));

        validateRequest(request);

        paymentRepository.findByUserIdAndIdempotencyKey(user.getId(), request.idempotencyKey().trim())
            .ifPresent(existing -> {
                throw new DuplicatePaymentRequestException("Duplicate payment request for idempotency key: " + request.idempotencyKey());
            });

        BigDecimal creditsGranted = request.creditsToPurchase();
        BigDecimal amountPaid = creditsGranted.multiply(CREDIT_PRICE_USD);

        CreditTopUpPayment payment = CreditTopUpPayment.builder()
            .user(user)
            .amountPaid(amountPaid)
            .creditsGranted(creditsGranted)
            .cardholderName(request.cardholderName().trim())
            .cardBrand(resolveCardBrand(request.cardNumber()))
            .cardLast4(extractLast4(request.cardNumber()))
            .expiryMonth(request.expiryMonth())
            .expiryYear(request.expiryYear())
            .idempotencyKey(request.idempotencyKey().trim())
            .status(CreditTopUpPaymentStatus.COMPLETED)
            .build();

        user.setCreditBalance(user.getCreditBalance().add(creditsGranted));
        userService.save(user);

        CreditTopUpPayment savedPayment = paymentRepository.save(payment);
        log.info(
            "Credit top-up completed userId={} keycloakId={} paymentId={} amountPaid={} creditsGranted={} status={}",
            user.getId(),
            keycloakId,
            savedPayment.getId(),
            amountPaid,
            creditsGranted,
            savedPayment.getStatus()
        );
        return savedPayment;
    }

    @Transactional(readOnly = true)
    public List<CreditTopUpPayment> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void validateRequest(CreateCreditTopUpCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request is required");
        }

        if (request.cardholderName() == null || request.cardholderName().isBlank()) {
            throw new IllegalArgumentException("Cardholder name is required");
        }
        if (request.cardNumber() == null || request.cardNumber().isBlank()) {
            throw new IllegalArgumentException("Card number is required");
        }
        if (request.cvv() == null || request.cvv().isBlank()) {
            throw new IllegalArgumentException("CVV is required");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (request.expiryMonth() == null || request.expiryMonth() < 1 || request.expiryMonth() > 12) {
            throw new IllegalArgumentException("Expiry month must be between 1 and 12");
        }
        if (request.expiryYear() == null) {
            throw new IllegalArgumentException("Expiry year is required");
        }
        LocalDate date = LocalDate.now();
        if (request.expiryYear() < date.getYear() || (request.expiryYear() == date.getYear() && request.expiryMonth() < date.getMonthValue())) {
            throw new IllegalArgumentException("Card has expired");
        }
        if (request.creditsToPurchase() == null || request.creditsToPurchase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credits to purchase must be greater than 0");
        }
        if (request.creditsToPurchase().compareTo(MAX_CREDITS_PER_TRANSACTION) > 0) {
            throw new IllegalArgumentException("Credits to purchase must not exceed 1000 per transaction");
        }

        String cardNumberDigits = request.cardNumber().replaceAll("\\D", "");
        if (!cardNumberDigits.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("Card number must contain between 13 and 19 digits");
        }

        String cvvDigits = request.cvv().trim();
        if (!cvvDigits.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("CVV must be 3 or 4 digits");
        }
    }

    private String extractLast4(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number must contain at least 4 digits");
        }
        return digits.substring(digits.length() - 4);
    }

    private String resolveCardBrand(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.startsWith("4")) {
            return "VISA";
        }
        if (digits.matches("^(5[1-5].*|2[2-7].*)$")) {
            return "MASTERCARD";
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return "AMEX";
        }
        if (digits.startsWith("6")) {
            return "DISCOVER";
        }
        return "UNKNOWN";
    }
}
