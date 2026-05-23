package org.gitbounty.gitbountybackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.gitbounty.gitbountybackend.util.KeycloakJwtConverterUtil;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                   KeycloakAuthenticationProvider keycloakAuthenticationProvider) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/health").permitAll()
                // Swagger/OpenAPI UI paths
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/git/**").authenticated()
                .anyRequest().permitAll()
            )
            // Keep HTTP Basic enabled so the Git servlet can authenticate using username/password
            .httpBasic(Customizer.withDefaults())
            // Also accept Bearer JWTs from Keycloak for API requests
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(KeycloakJwtConverterUtil.createConverter())))
            // register our custom provider so HTTP Basic credentials can be validated against Keycloak
            .authenticationProvider(keycloakAuthenticationProvider);

        return http.build();
    }
}




