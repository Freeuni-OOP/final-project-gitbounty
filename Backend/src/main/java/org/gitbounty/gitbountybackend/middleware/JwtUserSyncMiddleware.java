package org.gitbounty.gitbountybackend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtUserSyncMiddleware extends OncePerRequestFilter {

    private final UserService userService;

    public JwtUserSyncMiddleware(UserService userService){
        this.userService = userService;
    }
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Extract token and delegate syncing responsibilities to business layer
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userService.syncKeycloakUser(jwt);
        }
        filterChain.doFilter(request, response);
    }
}