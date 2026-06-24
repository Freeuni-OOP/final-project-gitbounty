package org.gitbounty.gitbountybackend.config;

import org.gitbounty.gitbountybackend.middleware.JwtUserSyncMiddleware;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MiddlewareConfig {

    private final UserService userService;

    public MiddlewareConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public FilterRegistrationBean<JwtUserSyncMiddleware> jwtUserSyncFilter() {
        FilterRegistrationBean<JwtUserSyncMiddleware> registrationBean = new FilterRegistrationBean<>();

        // Manual instantiation avoids circular dependency issues
        registrationBean.setFilter(new JwtUserSyncMiddleware(userService));

        // Restrict this middleware ONLY to API routes
        registrationBean.addUrlPatterns("/api/*");

        // Ensure this runs after the Spring Security filter chain has finished
        // Spring Security usually completes at order 0 or similar, so 10 is safe
        registrationBean.setOrder(10);

        return registrationBean;
    }
}