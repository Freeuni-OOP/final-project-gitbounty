package org.gitbounty.gitbountybackend.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;



@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class IssueJPATests {

    @Autowired
    private TestEntityManager entityManager;
    // REUSABLE HELPER METHODS (Fixed for Required User Fields)

    private User createAndPersistTestAuthor() {
        User author = new User();
        long timestamp = System.currentTimeMillis();

        author.setUsername("tester_" + timestamp);
        author.setEmail("tester_" + timestamp + "@gitbounty.com");
        author.setKeycloakId("keycloak_" + timestamp);

        return entityManager.persist(author);
    }

    private Codebase createAndPersistTestRepository(User owner) {
        Codebase repo = new Codebase();
        repo.setName("gitbounty-core-" + System.currentTimeMillis());
        repo.setGitUrl("https://github.com/gitbounty/core");
        repo.setOwner(owner);
        return entityManager.persist(repo);
    }

    private Issue createValidBaseIssue(User author, Codebase repo) {
        Issue issue = new Issue();
        issue.setTitle("Fix UI Alignment");
        issue.setDescription("The sidebar overlaps the dashboard main menu container.");
        issue.setAuthor(author);
        issue.setRepository(repo);
        issue.setNumber(1);
        return issue;
    }

    // 1. BASIC CRUD & ID MAPPING TESTS

    @Test
    public void shouldGenerateIdAutomaticallyOnSave() {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        assertNull(issue.getId(), "ID must be null before database persistence");

        Issue savedIssue = entityManager.persistFlushFind(issue);

        assertNotNull(savedIssue.getId(), "Database must assign an auto-incremented primary key");
        assertTrue(savedIssue.getId() > 0, "Generated ID must be positive");
    }

    @Test
    public void shouldSaveAndRetrieveAllFieldsCorrectly() {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        Issue savedIssue = entityManager.persistFlushFind(issue);

        entityManager.clear(); // Clear persistence cache to force a real DB read

        Issue retrievedIssue = entityManager.find(Issue.class, savedIssue.getId());

        assertNotNull(retrievedIssue);
        assertEquals("Fix UI Alignment", retrievedIssue.getTitle());
        assertEquals("The sidebar overlaps the dashboard main menu container.", retrievedIssue.getDescription());
    }

    // 2. COLUMN CONSTRAINT TESTS (nullable = false)

    @Test
    public void shouldThrowExceptionWhenTitleIsNull() {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        issue.setTitle(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(issue);
        }, "Database or Hibernate should reject persistence because title is missing");
    }

    @Test
    public void shouldApplyDefaultStatusOfOpen() {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        Issue savedIssue = entityManager.persistFlushFind(issue);

        assertEquals(IssueStatus.OPEN, savedIssue.getStatus(), "Status column must fallback to 'OPEN' by default");
    }

    // 3. RELATIONSHIP CONSTRAINT TESTS (ManyToOne)

    @Test
    public void shouldThrowExceptionWhenAuthorIsNull() {
        User owner = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(owner);

        Issue issue = createValidBaseIssue(null, repo);
        issue.setAuthor(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(issue);
        }, "Should reject save when mandatory author foreign key is missing");
    }

    @Test
    public void shouldThrowExceptionWhenRepositoryIsNull() {
        User author = createAndPersistTestAuthor();
        Issue issue = createValidBaseIssue(author, null);
        issue.setRepository(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(issue);
        }, "Should reject save when mandatory repository foreign key is missing");
    }

    // 4. LIFECYCLE AUDITING HOOKS TESTS

    @Test
    public void shouldPopulateTimestampsAutomaticallyOnCreate() {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        assertNull(issue.getCreatedAt());
        assertNull(issue.getUpdatedAt());

        Issue savedIssue = entityManager.persistFlushFind(issue);

        assertNotNull(savedIssue.getCreatedAt(), "createdAt should be auto-populated");
        assertNotNull(savedIssue.getUpdatedAt(), "updatedAt should be auto-populated");

        assertFalse(savedIssue.getUpdatedAt().isBefore(savedIssue.getCreatedAt()),
                "updatedAt timestamp must not be chronologically before createdAt");
    }

    @Test
    public void shouldAdvanceUpdatedAtTimestampOnUpdate() throws InterruptedException {
        User author = createAndPersistTestAuthor();
        Codebase repo = createAndPersistTestRepository(author);
        Issue issue = createValidBaseIssue(author, repo);

        Issue savedIssue = entityManager.persistFlushFind(issue);
        LocalDateTime originalCreatedAt = savedIssue.getCreatedAt();
        LocalDateTime originalUpdatedAt = savedIssue.getUpdatedAt();

        Thread.sleep(30);

        savedIssue.setTitle("Completely Revised Title Setup");
        Issue updatedIssue = entityManager.persistFlushFind(savedIssue);

        assertEquals(originalCreatedAt, updatedIssue.getCreatedAt());
        assertTrue(updatedIssue.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    // 5. EXPLICIT GETTER/SETTER COVERAGE TESTS

    @Test
    public void shouldManuallySetAndGetEntityBoilerplate() {
        Issue issue = new Issue();
        LocalDateTime testTime = LocalDateTime.of(2026, 1, 1, 12, 0);

        issue.setId(999L);
        issue.setCreatedAt(testTime);
        issue.setUpdatedAt(testTime);

        assertEquals(999L, issue.getId(), "Manual ID setter/getter failed");
        assertEquals(testTime, issue.getCreatedAt(), "Manual createdAt setter/getter failed");
        assertEquals(testTime, issue.getUpdatedAt(), "Manual updatedAt setter/getter failed");
    }


    // 6. GUARANTEED POJO COVERAGE TESTS (No Database)

    @Test
    public void shouldStrictlyCoverAuthorGetterAndSetter() {
        Issue issue = new Issue();
        User mockAuthor = new User();
        mockAuthor.setUsername("pure_test_author");

        issue.setAuthor(mockAuthor);

        assertNotNull(issue.getAuthor(), "Author should not be null after setter is called");
        assertEquals("pure_test_author", issue.getAuthor().getUsername(), "getAuthor did not return the exact object");
    }

    @Test
    public void shouldStrictlyCoverRepositoryGetterAndSetter() {
        Issue issue = new Issue();
        Codebase mockRepo = new Codebase();
        mockRepo.setName("pure_test_repo");

        issue.setRepository(mockRepo);

        assertNotNull(issue.getRepository(), "Repository should not be null after setter is called");
        assertEquals("pure_test_repo", issue.getRepository().getName(), "getRepository did not return the exact object");
    }

    @Test
    public void shouldStrictlyCoverStatusGetterAndSetter() {
        Issue issue = new Issue();

        issue.setStatus(IssueStatus.IN_PROGRESS);
        assertEquals(IssueStatus.IN_PROGRESS, issue.getStatus(), "getStatus did not return the manually set status");
    }

    @Test
    void shouldSetAndGetIssueNumber() {
        Issue issue = new Issue();

        issue.setNumber(1);
        assertEquals(1, issue.getNumber());

        issue.setNumber(2);
        assertEquals(2, issue.getNumber());

        issue.setNumber(57);
        assertEquals(57, issue.getNumber());
    }
}