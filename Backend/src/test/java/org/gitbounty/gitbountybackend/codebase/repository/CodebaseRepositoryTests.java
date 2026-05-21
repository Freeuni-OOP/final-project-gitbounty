package org.gitbounty.gitbountybackend.codebase.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CodebaseRepositoryTests {

    @Autowired
    private CodebaseRepository codebaseRepository;

    @Autowired
    private UserRepository userRepository;

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private User createOwner() {
        return userRepository.saveAndFlush(
            new User("owner_" + randomSuffix(), randomSuffix() + "@test.local")
        );
    }

    @Test
    void canSaveAndRetrieveCodebase() {
        User owner = createOwner();
        String name = "codebase_" + randomSuffix();

        Codebase saved = codebaseRepository.saveAndFlush(
            new Codebase(name, "Payments API", "git@github.com:gitbounty/payments.git", owner)
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo(name);
        assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(saved.getCreatedAt()).isNotNull();

        var found = codebaseRepository.findByName(name);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getOwner().getUsername()).isEqualTo(owner.getUsername());
    }

    @Test
    void findByNameReturnsEmptyWhenNotFound() {
        assertThat(codebaseRepository.findByName("missing_" + randomSuffix())).isEmpty();
    }

    @Test
    void cannotSaveDuplicateCodebaseName() {
        User owner = createOwner();
        String name = "codebase_" + randomSuffix();

        codebaseRepository.saveAndFlush(
            new Codebase(name, "First", "git@github.com:gitbounty/first.git", owner)
        );

        Codebase duplicate = new Codebase(
            name,
            "Second",
            "git@github.com:gitbounty/second.git",
            owner
        );

        assertThatThrownBy(() -> codebaseRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void codebaseRepositoryIsAvailable() {
        assertThat(codebaseRepository).isNotNull();
    }
}


