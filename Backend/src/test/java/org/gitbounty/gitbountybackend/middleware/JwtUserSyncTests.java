package org.gitbounty.gitbountybackend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtUserSyncTests {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private JwtUserSyncMiddleware jwtUserSyncMiddleware;

    @BeforeEach
    void setUp() {
        // Clear the SecurityContext before each test to guarantee an isolated test state
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clean up context after test execution
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSyncUserWhenJwtIsPresentInSecurityContext() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        jwtUserSyncMiddleware.doFilterInternal(request, response, filterChain);

        // Then
        verify(userService, times(1)).syncKeycloakUser(jwt);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldSkipSyncWhenNoAuthenticationIsPresent() throws Exception {
        // Given
        SecurityContextHolder.getContext().setAuthentication(null);

        // When
        jwtUserSyncMiddleware.doFilterInternal(request, response, filterChain);

        // Then
        verify(userService, never()).syncKeycloakUser(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldSkipSyncWhenPrincipalIsNotAJwt() throws Exception {
        // Given
        // Simulates an unexpected principal type, like a simple String username username
        when(authentication.getPrincipal()).thenReturn("not-a-jwt-object");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        jwtUserSyncMiddleware.doFilterInternal(request, response, filterChain);

        // Then
        verify(userService, never()).syncKeycloakUser(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}