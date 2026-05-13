package org.gitbounty.gitbountybackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GitBounty API")
                .version("0.0.1")
                .description("REST API for GitBounty backend - manage repositories and users")
            );
    }
}

