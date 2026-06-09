package org.gitbounty.gitbountybackend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // Gives us full access to MockMvc inside a real Spring Context
@ActiveProfiles("dev")
class UserControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    // Mock only the external decoder engine to control token validation
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private String randomValue(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private Jwt jwtFor(String token, String keycloakId, String username, String email) {
        return Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject(keycloakId)
            .claim("preferred_username", username)
            .claim("email", email)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    @Test
    void getUserByIdReturns401WhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByIdReturns401WhenTokenIsInvalid() throws Exception {
        String badToken = "fake-expired-token";
        when(jwtDecoder.decode(badToken))
            .thenThrow(new OAuth2AuthenticationException("invalid token"));

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer " + badToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserProfileReturns200ForAuthenticatedUser() throws Exception {
        String token = randomValue("token_");
        String keycloakId = randomValue("kc_");
        String username = randomValue("usr_");
        String email = randomValue("mail_") + "@test.local";

        userService.save(new User(username, email, keycloakId));
        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token, keycloakId, username, email));

        mockMvc.perform(get("/api/users/profile/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void updateUserProfileReturns200WhenOwnerUpdatesOwnProfile() throws Exception {
        String token = randomValue("token_");
        String keycloakId = randomValue("kc_");
        String username = randomValue("owner_");
        String email = randomValue("mail_") + "@test.local";

        User owner = userService.save(new User(username, email, keycloakId));
        String updatedUsername = randomValue("owner_new_");
        String updatedEmail = randomValue("new_mail_") + "@test.local";

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token, keycloakId, username, email));

        mockMvc.perform(put("/api/users/{id}", owner.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + updatedUsername + "\",\"email\":\"" + updatedEmail + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(owner.getId()))
            .andExpect(jsonPath("$.username").value(updatedUsername))
            .andExpect(jsonPath("$.email").value(updatedEmail));
    }

    @Test
    void updateUserProfileReturns403WhenNonOwnerAttemptsUpdate() throws Exception {
        String ownerKeycloakId = randomValue("kc_owner_");
        String ownerUsername = randomValue("owner_");
        String ownerEmail = randomValue("owner_mail_") + "@test.local";
        User owner = userService.save(new User(ownerUsername, ownerEmail, ownerKeycloakId));

        String intruderToken = randomValue("token_intruder_");
        String intruderKeycloakId = randomValue("kc_intruder_");
        String intruderUsername = randomValue("intruder_");
        String intruderEmail = randomValue("intruder_mail_") + "@test.local";
        userService.save(new User(intruderUsername, intruderEmail, intruderKeycloakId));

        when(jwtDecoder.decode(intruderToken))
            .thenReturn(jwtFor(intruderToken, intruderKeycloakId, intruderUsername, intruderEmail));

        mockMvc.perform(put("/api/users/{id}", owner.getId())
                .header("Authorization", "Bearer " + intruderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ignored\",\"email\":\"ignored@test.local\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUserProfileReturns409WhenUsernameAlreadyExists() throws Exception {
        String ownerKeycloakId = randomValue("kc_owner_");
        String ownerUsername = randomValue("owner_");
        String ownerEmail = randomValue("owner_mail_") + "@test.local";
        User owner = userService.save(new User(ownerUsername, ownerEmail, ownerKeycloakId));

        User existing = userService.save(new User(randomValue("existing_"), randomValue("existing_mail_") + "@test.local", randomValue("kc_existing_")));

        String token = randomValue("token_owner_");
        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token, ownerKeycloakId, ownerUsername, ownerEmail));

        mockMvc.perform(put("/api/users/{id}", owner.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + existing.getUsername() + "\",\"email\":\"" + ownerEmail + "\"}"))
            .andExpect(status().isConflict());
    }
}