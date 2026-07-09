package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.commit.CommitRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.transaction.TransactionRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Exercises the deletion cascade against a REAL Hibernate persistence context
 * (as opposed to the Mockito-based unit tests in CodebaseDeletionServiceTests),
 * because the bug this covers is a Hibernate flush/first-level-cache staleness
 * issue that mocked repositories can never reproduce.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:codebase-deletion-persistence-schema.sql",
        "spring.datasource.url=jdbc:h2:mem:codebase-deletion-tests;DB_CLOSE_DELAY=-1"
})
class CodebaseDeletionServicePersistenceTests {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private CodebaseRepository codebaseRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private BountyRepository bountyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    private CodebaseDeletionService deletionService;
    private User owner;

    @BeforeEach
    void setUp() {
        // UserService's constructor is package-private and unused by findById() anyway - mock it.
        BranchService branchService = new BranchService(branchRepository, commitRepository);
        CodebaseService codebaseService =
                new CodebaseService(codebaseRepository, mock(CodebaseStorageService.class), mock(UserService.class), branchService);
        TransactionService transactionService = new TransactionService(transactionRepository, userRepository, issueRepository);
        BountyService bountyService =
                new BountyService(bountyRepository, issueRepository, userRepository, transactionService, mock(IssueService.class));

        deletionService = new CodebaseDeletionService(
                codebaseService, codebaseRepository, bountyService, transactionService, issueRepository, branchService,
                entityManager);

        owner = new User("owner", "owner@test.com", "kc-owner");
        owner.setCreditBalance(BigDecimal.valueOf(100));
        owner = userRepository.saveAndFlush(owner);
    }

    private Codebase createCodebase() {
        Codebase codebase = new Codebase("demo", "desc", "http://localhost/git/demo.git", owner);
        return codebaseRepository.saveAndFlush(codebase);
    }

    private Issue createIssue(Codebase codebase, int number) {
        Issue issue = new Issue();
        issue.setNumber(number);
        issue.setTitle("Issue " + number);
        issue.setAuthor(owner);
        issue.setRepository(codebase);
        return issueRepository.saveAndFlush(issue);
    }

    private Bounty createEscrowedBounty(Issue issue, BountyStatus status, double amount) {
        // Mirrors what BountyService.createBounty does: debit the owner's balance
        // to move funds into escrow before the bounty exists.
        owner.setCreditBalance(owner.getCreditBalance().subtract(BigDecimal.valueOf(amount)));
        userRepository.saveAndFlush(owner);

        Bounty bounty = new Bounty();
        bounty.setTitle("Bounty for " + issue.getTitle());
        bounty.setAmount(amount);
        bounty.setStatus(status);
        bounty.setIssue(issue);
        bounty.setCreatedAt(LocalDateTime.now());
        return bountyRepository.saveAndFlush(bounty);
    }

    @Test
    void deleteRepositoryRecords_ShouldNotThrow_WhenRepoHasOpenEscrowedBounty() {
        Codebase codebase = createCodebase();
        Issue issue = createIssue(codebase, 1);
        createEscrowedBounty(issue, BountyStatus.OPEN, 40.0);

        assertThatCode(() -> {
            deletionService.deleteRepositoryRecords(codebase.getId());
            testEntityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(codebaseRepository.findById(codebase.getId())).isEmpty();
        assertThat(issueRepository.findById(issue.getId())).isEmpty();

        BigDecimal refundedBalance = userRepository.findById(owner.getId()).orElseThrow().getCreditBalance();
        assertThat(refundedBalance).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void deleteRepositoryRecords_ShouldNotThrow_WhenRepoHasAssignedEscrowedBounty() {
        Codebase codebase = createCodebase();
        Issue issue = createIssue(codebase, 1);
        createEscrowedBounty(issue, BountyStatus.ASSIGNED, 25.0);

        assertThatCode(() -> {
            deletionService.deleteRepositoryRecords(codebase.getId());
            testEntityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(codebaseRepository.findById(codebase.getId())).isEmpty();
        assertThat(issueRepository.findById(issue.getId())).isEmpty();

        BigDecimal refundedBalance = userRepository.findById(owner.getId()).orElseThrow().getCreditBalance();
        assertThat(refundedBalance).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
