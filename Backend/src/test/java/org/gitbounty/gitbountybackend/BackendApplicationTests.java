package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class BackendApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BackendApplication backendApplication;

    @Test
    void contextLoads() {
        assertThat(backendApplication).isNotNull();
    }

    @Test
    void applicationContextIsNotNull() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void backendApplicationBeanExists() {
        assertThat(applicationContext.containsBean("backendApplication")).isTrue();
    }

    @Test
    void applicationNameIsConfigured() {
        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        assertThat(appName).isEqualTo("Backend");
    }

    @Test
    void serverPortIsConfigured() {
        String port = applicationContext.getEnvironment().getProperty("server.port");
        assertThat(port).isEqualTo("8081");
    }
}




