package org.gitbounty.gitbountybackend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class CodebaseEntityTests {

    @Test
    void canInstantiateCodebaseWithNoArgsConstructor() {
        Codebase codebase = new Codebase();

        assertThat(codebase).isNotNull();
    }
    private String randomKeycloakId() {
        return "kc_" + UUID.randomUUID().toString().substring(0, 8);
    }
    @Test
    void codebaseConstructorSetsFields() {
        User owner = new User("owner_123", "owner_123@test.local",randomKeycloakId() );

        Codebase codebase = new Codebase(
            "Payment-Gateway-API",
            "Payment gateway service repository",
            "git@provider.com:user/repo.git",
            owner
        );

        assertThat(codebase.getName()).isEqualTo("Payment-Gateway-API");
        assertThat(codebase.getDescription()).isEqualTo("Payment gateway service repository");
        assertThat(codebase.getGitUrl()).isEqualTo("git@provider.com:user/repo.git");
        assertThat(codebase.getOwner()).isEqualTo(owner);
    }

    @Test
    void codebaseGettersAndSettersWork() {
        User owner = new User("owner_456", "owner_456@test.local", randomKeycloakId());
        Codebase codebase = new Codebase();

        codebase.setId(7L);
        codebase.setName("Search-Service");
        codebase.setDescription("Search service repo");
        codebase.setGitUrl("git@provider.com:user/search-service.git");
        codebase.setOwner(owner);

        assertThat(codebase.getId()).isEqualTo(7L);
        assertThat(codebase.getName()).isEqualTo("Search-Service");
        assertThat(codebase.getDescription()).isEqualTo("Search service repo");
        assertThat(codebase.getGitUrl()).isEqualTo("git@provider.com:user/search-service.git");
        assertThat(codebase.getOwner()).isEqualTo(owner);
    }

    @Test
    void codebaseToStringIsReadable() {
        Codebase codebase = new Codebase(
            "Auth-Service",
            "Authentication repo",
            "git@provider.com:user/auth-service.git",
            new User("owner_789", "owner_789@test.local", randomKeycloakId())
        );

        assertThat(codebase.toString())
            .contains("Auth-Service")
            .contains("git@provider.com:user/auth-service.git");
    }
}


