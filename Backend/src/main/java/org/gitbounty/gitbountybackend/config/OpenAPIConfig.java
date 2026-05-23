package org.gitbounty.gitbountybackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    @Bean
    public OpenAPI customOpenAPI() {
        // Construct the URLs based on your existing Keycloak realm configuration
        String authUrl = issuerUri + "/protocol/openid-connect/auth";
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        return new OpenAPI()
            .info(new Info()
                .title("GitBounty API")
                .version("1.0")
                .description("REST API specifications secured by Keycloak OIDC"))
            // Global requirement: applies this security configuration to ALL endpoints automatically
            .addSecurityItem(new SecurityRequirement().addList("KeycloakAuth"))
            .components(new Components()
                .addSecuritySchemes("KeycloakAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("Keycloak OpenID Connect Authentication")
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl(authUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(new Scopes()
                                .addString("openid", "Standard OpenID constraints")
                            )
                        )
                    )
                )
            );
    }
}

