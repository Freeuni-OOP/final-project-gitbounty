package org.gitbounty.gitbountybackend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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

    // Mock only the external decoder engine to control token validation
    @MockitoBean
    private JwtDecoder jwtDecoder;

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

}