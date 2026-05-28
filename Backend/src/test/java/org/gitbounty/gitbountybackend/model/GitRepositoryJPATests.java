package org.gitbounty.gitbountybackend.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.gitbounty.gitbountybackend.repository.GitRepositoryRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GitRepositoryJPATests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GitRepositoryRepository repository;

    // REUSABLE HELPER METHODS

    private GitRepository createValidBaseRepository() {
        GitRepository repo = new GitRepository();
        repo.setName("gitbounty-frontend-" + System.currentTimeMillis());
        repo.setUrl("https://github.com/gitbounty/frontend");
        repo.setDescription("The main React frontend application");
        return repo;
    }

    // 1. BASIC CRUD & ID MAPPING TESTS

    @Test
    void shouldGenerateIdAutomaticallyOnSave() {
        GitRepository repo = createValidBaseRepository();
        assertNull(repo.getId(), "ID must be null before database persistence");

        GitRepository savedRepo = entityManager.persistFlushFind(repo);

        assertNotNull(savedRepo.getId(), "Database must assign an auto-incremented primary key");
        assertTrue(savedRepo.getId() > 0, "Generated ID must be positive");
    }

    @Test
    void shouldSaveAndRetrieveAllFieldsCorrectly() {
        GitRepository repo = createValidBaseRepository();
        GitRepository savedRepo = entityManager.persistFlushFind(repo);

        entityManager.clear();

        GitRepository retrievedRepo = entityManager.find(GitRepository.class, savedRepo.getId());

        assertNotNull(retrievedRepo);
        assertEquals(savedRepo.getName(), retrievedRepo.getName());
        assertEquals("https://github.com/gitbounty/frontend", retrievedRepo.getUrl());
        assertEquals("The main React frontend application", retrievedRepo.getDescription());
    }

    // 2. COLUMN CONSTRAINT TESTS

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        GitRepository repo = createValidBaseRepository();
        repo.setName(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(repo);
        }, "Hibernate should reject persistence because name is null");
    }

    @Test
    void shouldThrowExceptionWhenUrlIsNull() {
        GitRepository repo = createValidBaseRepository();
        repo.setUrl(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(repo);
        }, "Hibernate should reject persistence because url is null");
    }

    @Test
    void shouldThrowExceptionWhenNameIsNotUnique() {
        GitRepository firstRepo = createValidBaseRepository();
        firstRepo.setName("duplicate-repo-name");
        entityManager.persistAndFlush(firstRepo);

        GitRepository secondRepo = createValidBaseRepository();
        secondRepo.setName("duplicate-repo-name");

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(secondRepo);
        }, "Database must reject save when unique=true constraint on 'name' is violated");
    }

    // 3. CUSTOM REPOSITORY METHOD TESTS

    @Test
    void shouldFindRepositoryByName() {
        GitRepository repo = createValidBaseRepository();
        repo.setName("unique-searchable-name");
        entityManager.persistAndFlush(repo);

        Optional<GitRepository> found = repository.findByName("unique-searchable-name");

        assertTrue(found.isPresent(), "Repository should be found by its unique name");
        assertEquals("unique-searchable-name", found.get().getName());
    }

    // 4. LIFECYCLE AUDITING HOOKS TESTS

    @Test
    void shouldPopulateTimestampsAutomaticallyOnCreate() {
        GitRepository repo = createValidBaseRepository();

        assertNull(repo.getCreatedAt());
        assertNull(repo.getUpdatedAt());

        GitRepository savedRepo = entityManager.persistFlushFind(repo);

        assertNotNull(savedRepo.getCreatedAt(), "createdAt should be auto-populated");
        assertNotNull(savedRepo.getUpdatedAt(), "updatedAt should be auto-populated");
        assertFalse(savedRepo.getUpdatedAt().isBefore(savedRepo.getCreatedAt()),
                "updatedAt timestamp must not be chronologically before createdAt");
    }

    @Test
    void shouldAdvanceUpdatedAtTimestampOnUpdate() throws InterruptedException {
        GitRepository repo = createValidBaseRepository();
        GitRepository savedRepo = entityManager.persistFlushFind(repo);

        LocalDateTime originalCreatedAt = savedRepo.getCreatedAt();
        LocalDateTime originalUpdatedAt = savedRepo.getUpdatedAt();

        Thread.sleep(30);

        savedRepo.setDescription("Updated Description");
        GitRepository updatedRepo = entityManager.persistFlushFind(savedRepo);

        assertEquals(originalCreatedAt, updatedRepo.getCreatedAt(), "createdAt must not change on update");
        assertTrue(updatedRepo.getUpdatedAt().isAfter(originalUpdatedAt), "updatedAt must advance forward");
    }

    // 5. GUARANTEED POJO COVERAGE TESTS (No Database)

    @Test
    void shouldStrictlyCoverAllGettersAndSetters() {
        GitRepository repo = new GitRepository();
        LocalDateTime testTime = LocalDateTime.of(2026, 5, 29, 12, 0);

        repo.setId(100L);
        repo.setName("test-pojo-repo");
        repo.setUrl("https://test.com/repo");
        repo.setDescription("pojo description");
        repo.setCreatedAt(testTime);
        repo.setUpdatedAt(testTime);

        assertEquals(100L, repo.getId());
        assertEquals("test-pojo-repo", repo.getName());
        assertEquals("https://test.com/repo", repo.getUrl());
        assertEquals("pojo description", repo.getDescription());
        assertEquals(testTime, repo.getCreatedAt());
        assertEquals(testTime, repo.getUpdatedAt());
    }
}