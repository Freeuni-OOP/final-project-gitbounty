package org.gitbounty.gitbountybackend.service.codebase;

import jakarta.persistence.EntityManager;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.model.TransactionStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchRepository;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.commit.CommitRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionRepository;
import org.gitbounty.gitbountybackend.service.transaction.TransactionService;
import org.gitbounty.gitbountybackend.service.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Exercises the deletion cascade against a REAL Hibernate persistence context (as opposed
 * to the Mockito-based unit tests in CodebaseDeletionCascadeServiceTests), because the bugs
 * this covers are Hibernate flush/first-level-cache behaviors that mocked repositories can
 * never reproduce - most importantly, that CodebaseDeletionCascadeService's explicit
 * per-issue bountyService.cancelIfActive(...) call actually refunds an active bounty and
 * leaves the database in a consistent state once that issue (and its cascaded bounty) is
 * deleted through normal entity-level removal.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:codebase-deletion-persistence-schema.sql",
        "spring.datasource.url=jdbc:h2:mem:codebase-deletion-cascade-tests;DB_CLOSE_DELAY=-1"
})
class CodebaseDeletionCascadeServicePersistenceTests {

    @TestConfiguration
    static class RealServiceBeans {
        @Bean
        TransactionService transactionService(TransactionRepository transactionRepository,
                                               UserRepository userRepository,
                                               IssueRepository issueRepository) {
            return new TransactionService(transactionRepository, userRepository, issueRepository);
        }

        @Bean
        BountyService bountyService(BountyRepository bountyRepository,
                                     IssueRepository issueRepository,
                                     UserRepository userRepository,
                                     TransactionService transactionService) {
            // IssueService is only used by claimBounty/unclaimBounty, neither of which this
            // cascade touches - mock it rather than wiring its whole dependency chain.
            return new BountyService(bountyRepository, issueRepository, userRepository, transactionService,
                    mock(IssueService.class));
        }
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CodebaseRepository codebaseRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private PullRequestRepository pullRequestRepository;

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

    // The real beans from RealServiceBeans above - used directly by the cascade service
    // below so its explicit bountyService.cancelIfActive(...) call goes through the same
    // TransactionService/repositories this test also uses directly.
    @Autowired
    private TransactionService transactionServiceBean;

    @Autowired
    private BountyService bountyServiceBean;

    private CodebaseDeletionCascadeService cascadeService;
    private User owner;

    @BeforeEach
    void setUp() {
        BranchService branchService = new BranchService(branchRepository, commitRepository);
        cascadeService = new CodebaseDeletionCascadeService(codebaseRepository, branchService, transactionServiceBean,
                issueRepository, pullRequestRepository, entityManager, bountyServiceBean);

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
        Bounty bounty = new Bounty();
        bounty.setTitle("Bounty for " + issue.getTitle());
        bounty.setAmount(amount);
        bounty.setStatus(status);
        bounty.setIssue(issue);
        bounty.setCreatedAt(LocalDateTime.now());
        bounty = bountyRepository.saveAndFlush(bounty);

        // Mirrors what BountyService.createBounty does in production: records a real
        // deposit Transaction referencing this bounty and debits the owner's balance. This
        // is the pre-existing transaction that
        // transactionService.detachBountyReferencesForRepository must sever before the
        // bounty row is deleted - without it, deleting the bounty (cascaded from its
        // issue) would violate transactions.bounty_id's foreign key.
        transactionServiceBean.recordBountyDeposit(owner, bounty, BigDecimal.valueOf(amount),
                "Bounty deposit for issue #" + issue.getNumber() + ": " + issue.getTitle());

        return bounty;
    }

    @Test
    void deleteRepositoryRecords_ShouldNotThrow_WhenRepoHasNoIssuesOrBounties() {
        Codebase codebase = createCodebase();

        assertThatCode(() -> {
            cascadeService.deleteRepositoryRecords(codebase.getId());
            entityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(codebaseRepository.findById(codebase.getId())).isEmpty();
    }

    @Test
    void deleteRepositoryRecords_ShouldRefundViaExplicitCancelIfActive_WhenRepoHasOpenEscrowedBounty() {
        Codebase codebase = createCodebase();
        Issue issue = createIssue(codebase, 1);
        Bounty bounty = createEscrowedBounty(issue, BountyStatus.OPEN, 40.0);
        Long depositTransactionId = transactionRepository.findByBountyIssueId(issue.getId()).stream().findFirst().orElseThrow().getId();

        assertThatCode(() -> {
            cascadeService.deleteRepositoryRecords(codebase.getId());
            entityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(codebaseRepository.findById(codebase.getId())).isEmpty();
        assertThat(issueRepository.findById(issue.getId())).isEmpty();
        assertThat(bountyRepository.findById(bounty.getId())).isEmpty();

        BigDecimal refundedBalance = userRepository.findById(owner.getId()).orElseThrow().getCreditBalance();
        assertThat(refundedBalance).isEqualByComparingTo(BigDecimal.valueOf(100));

        List<Transaction> refunds = transactionRepository.findByToUserIdAndStatus(owner.getId(), TransactionStatus.COMPLETED);
        assertThat(refunds).anyMatch(t -> t.getAmount().compareTo(BigDecimal.valueOf(40.0)) == 0);

        // The pre-existing deposit transaction must survive detached (bounty = null), not
        // deleted and not left pointing at a bounty row that no longer exists - this is
        // specifically what transactionService.detachBountyReferencesForRepository is for,
        // as distinct from cancelIfActive, which only ever handles the new refund
        // transaction it creates itself.
        Transaction depositTransaction = transactionRepository.findById(depositTransactionId).orElseThrow();
        assertThat(depositTransaction.getBounty()).isNull();
    }

    @Test
    void deleteRepositoryRecords_ShouldRefundViaExplicitCancelIfActive_WhenRepoHasAssignedEscrowedBounty() {
        Codebase codebase = createCodebase();
        Issue issue = createIssue(codebase, 1);
        Bounty bounty = createEscrowedBounty(issue, BountyStatus.ASSIGNED, 25.0);
        Long depositTransactionId = transactionRepository.findByBountyIssueId(issue.getId()).stream().findFirst().orElseThrow().getId();

        assertThatCode(() -> {
            cascadeService.deleteRepositoryRecords(codebase.getId());
            entityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(codebaseRepository.findById(codebase.getId())).isEmpty();
        assertThat(issueRepository.findById(issue.getId())).isEmpty();
        assertThat(bountyRepository.findById(bounty.getId())).isEmpty();

        BigDecimal refundedBalance = userRepository.findById(owner.getId()).orElseThrow().getCreditBalance();
        assertThat(refundedBalance).isEqualByComparingTo(BigDecimal.valueOf(100));

        Transaction depositTransaction = transactionRepository.findById(depositTransactionId).orElseThrow();
        assertThat(depositTransaction.getBounty()).isNull();
    }

    @Test
    void deleteRepositoryRecords_ShouldNotRefund_WhenBountyAlreadyCompleted() {
        Codebase codebase = createCodebase();
        Issue issue = createIssue(codebase, 1);
        Bounty bounty = createEscrowedBounty(issue, BountyStatus.COMPLETED, 15.0);

        assertThatCode(() -> {
            cascadeService.deleteRepositoryRecords(codebase.getId());
            entityManager.flush();
        }).doesNotThrowAnyException();

        assertThat(bountyRepository.findById(bounty.getId())).isEmpty();

        // Balance stays debited: a completed bounty was already paid out, so cancelIfActive
        // must not treat it as still-active and refund it a second time.
        BigDecimal balance = userRepository.findById(owner.getId()).orElseThrow().getCreditBalance();
        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(85.0));
    }
}
