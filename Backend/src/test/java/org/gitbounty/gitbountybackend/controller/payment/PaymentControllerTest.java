package org.gitbounty.gitbountybackend.controller.payment;

import org.gitbounty.gitbountybackend.config.KeycloakAuthenticationProvider;
import org.gitbounty.gitbountybackend.config.SecurityConfig;
import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;
import org.gitbounty.gitbountybackend.model.CreditTopUpPaymentStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.payment.CreditTopUpPaymentService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditTopUpPaymentService paymentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private KeycloakAuthenticationProvider keycloakAuthenticationProvider;

    private static final String VALID_TOP_UP_JSON = """
        {
            "creditsToPurchase": 100,
            "cardholderName": "Jane Doe",
            "cardNumber": "4242424242424242",
            "expiryMonth": 12,
            "expiryYear": 2030,
            "cvv": "123",
            "idempotencyKey": "key-123"
        }
        """;

    private static User testUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user-123");
        user.setEmail("user-123@example.com");
        user.setKeycloakId("user-123");
        user.setCreditBalance(new BigDecimal("50.00"));
        return user;
    }

    private static CreditTopUpPayment testPayment(User user) {
        return CreditTopUpPayment.builder()
                .id(10L)
                .user(user)
                .amountPaid(new BigDecimal("1000.00"))
                .creditsGranted(new BigDecimal("100.00"))
                .cardholderName("Jane Doe")
                .cardBrand("VISA")
                .cardLast4("4242")
                .expiryMonth(12)
                .expiryYear(2030)
                .idempotencyKey("key-123")
                .status(CreditTopUpPaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCreditTopUp_ShouldReturnCreated() throws Exception {
        CreditTopUpPayment payment = testPayment(testUser());

        when(paymentService.createMockTopUp(eq("user-123"), any())).thenReturn(payment);

        mockMvc.perform(post("/api/payments/credit-topups")
                        .with(jwt().jwt(builder -> builder.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOP_UP_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.cardLast4").value("4242"));

        verify(paymentService).createMockTopUp(eq("user-123"), any());
    }

    @Test
    void createCreditTopUp_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/payments/credit-topups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOP_UP_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void getMyPayments_ShouldReturnOk() throws Exception {
        User user = testUser();
        CreditTopUpPayment payment = testPayment(user);

        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(user));
        when(paymentService.getPaymentsForUser(1L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/me")
                        .with(jwt().jwt(builder -> builder.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(paymentService).getPaymentsForUser(1L);
    }

    @Test
    void getMyPayments_ShouldReturnNotFound_WhenUserMissing() throws Exception {
        when(userService.findByKeycloakId("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/me")
                        .with(jwt().jwt(builder -> builder.subject("ghost"))))
                .andExpect(status().isNotFound());

        verifyNoInteractions(paymentService);
    }

    @Test
    void getMyBalance_ShouldReturnOk() throws Exception {
        when(userService.findByKeycloakId("user-123")).thenReturn(Optional.of(testUser()));

        mockMvc.perform(get("/api/payments/me/balance")
                        .with(jwt().jwt(builder -> builder.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.creditBalance").value(50.00));
    }

    @Test
    void getMyBalance_ShouldReturnNotFound_WhenUserMissing() throws Exception {
        when(userService.findByKeycloakId("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/me/balance")
                        .with(jwt().jwt(builder -> builder.subject("ghost"))))
                .andExpect(status().isNotFound());
    }
}