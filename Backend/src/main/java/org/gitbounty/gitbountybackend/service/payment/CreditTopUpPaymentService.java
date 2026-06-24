package org.gitbounty.gitbountybackend.service.payment;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;
import org.gitbounty.gitbountybackend.model.CreditTopUpPaymentStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CreditTopUpPaymentRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CreditTopUpPaymentService {

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

        BigDecimal creditsGranted = request.creditsToPurchase();
        BigDecimal amountPaid = request.amountPaid();

        CreditTopUpPayment payment = CreditTopUpPayment.builder()
            .user(user)
            .amountPaid(amountPaid)
            .creditsGranted(creditsGranted)
            .cardholderName(request.cardholderName().trim())
            .cardBrand(resolveCardBrand(request.cardNumber()))
            .cardLast4(extractLast4(request.cardNumber()))
            .expiryMonth(request.expiryMonth())
            .expiryYear(request.expiryYear())
            .status(CreditTopUpPaymentStatus.COMPLETED)
            .build();

        user.setCreditBalance(user.getCreditBalance().add(creditsGranted));
        userService.save(user);

        return paymentRepository.save(payment);
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
        if (request.amountPaid() == null || request.amountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount paid must be greater than 0");
        }
        if (request.creditsToPurchase() == null || request.creditsToPurchase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credits to purchase must be greater than 0");
        }
        if (request.amountPaid().compareTo(request.creditsToPurchase()) != 0) {
            throw new IllegalArgumentException("For the mock payment flow, amount paid must equal credits purchased");
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
