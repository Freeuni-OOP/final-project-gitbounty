package org.gitbounty.gitbountybackend.controller.payment;

import org.gitbounty.gitbountybackend.controller.payment.dto.CreateCreditTopUpRequest;
import org.gitbounty.gitbountybackend.controller.payment.dto.CreditTopUpPaymentResponse;
import org.gitbounty.gitbountybackend.controller.transaction.dto.CreditBalanceResponse;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.payment.CreateCreditTopUpCommand;
import org.gitbounty.gitbountybackend.service.payment.CreditTopUpPaymentService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final CreditTopUpPaymentService paymentService;
    private final UserService userService;

    public PaymentController(CreditTopUpPaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping("/credit-topups")
    public ResponseEntity<CreditTopUpPaymentResponse> createCreditTopUp(
        @RequestBody CreateCreditTopUpRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CreditTopUpPaymentResponse response = CreditTopUpPaymentResponse.from(
            paymentService.createMockTopUp(
                jwt.getSubject(),
                new CreateCreditTopUpCommand(
                    request.creditsToPurchase(),
                    request.cardholderName(),
                    request.cardNumber(),
                    request.expiryMonth(),
                    request.expiryYear(),
                    request.cvv(),
                    request.idempotencyKey()
                )
            )
        );
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<CreditTopUpPaymentResponse>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        User user = getAuthenticatedUser(jwt);

        List<CreditTopUpPaymentResponse> responses = paymentService.getPaymentsForUser(user.getId())
            .stream()
            .map(CreditTopUpPaymentResponse::from)
            .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/me/balance")
    public ResponseEntity<CreditBalanceResponse> getMyBalance(@AuthenticationPrincipal Jwt jwt) {
        User user = getAuthenticatedUser(jwt);
        return ResponseEntity.ok(new CreditBalanceResponse(user.getId(), user.getCreditBalance()));
    }

    private User getAuthenticatedUser(Jwt jwt) {
        return userService.findByKeycloakId(jwt.getSubject())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
