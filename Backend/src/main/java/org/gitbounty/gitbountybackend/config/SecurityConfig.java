package org.gitbounty.gitbountybackend.config;

import org.gitbounty.gitbountybackend.middleware.JwtUserSyncMiddleware;
import org.gitbounty.gitbountybackend.util.KeycloakJwtConverterUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUserSyncMiddleware jwtUserSyncMiddleware;

    public SecurityConfig(JwtUserSyncMiddleware jwtUserSyncMiddleware) {
        this.jwtUserSyncMiddleware = jwtUserSyncMiddleware;
    }

    /**
     * CHAIN 1: Git Subsystem Security Context
     * Matches ONLY /git/**. Uses HTTP Basic backed by your Keycloak provider.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain gitSecurityFilterChain(HttpSecurity http,
                                                      KeycloakAuthenticationProvider keycloakAuthenticationProvider) {
        http
            .securityMatcher("/git/**") // This chain ONLY evaluates requests starting with /git/
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .authenticationProvider(keycloakAuthenticationProvider); // Isolated strictly to git

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // Open Public routes
                .requestMatchers("/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Secure API endpoints
                .requestMatchers("/api/**").authenticated()

                // Tight catch-all: Anything else hitting this backend must be rejected
                .anyRequest().denyAll()
            )
            // Pure JWT resource server - HTTP Basic cannot leak in here
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(KeycloakJwtConverterUtil.createConverter())
            ))
            // Process our JIT creation right after JWT parsing succeeds
            .addFilterAfter(jwtUserSyncMiddleware, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}