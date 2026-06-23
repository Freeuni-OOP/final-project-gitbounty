package org.gitbounty.gitbountybackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
            .csrf(AbstractHttpConfigurer::disable).securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                // Open Public routes
                .requestMatchers("/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bounties", "/api/bounties/status/**").permitAll()

                // Secure API endpoints
                .requestMatchers("/api/**").authenticated()

                // Tight catch-all: Anything else hitting this backend must be rejected
                .anyRequest().denyAll()
            )
            // Pure JWT resource server - HTTP Basic cannot leak in here
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}