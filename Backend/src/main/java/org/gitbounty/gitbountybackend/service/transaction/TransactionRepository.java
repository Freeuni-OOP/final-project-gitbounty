package org.gitbounty.gitbountybackend.service.transaction;

import java.util.List;
import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a specific user (received transactions)
     */
    List<Transaction> findByToUserId(Long toUserId);

    /**
     * Find all transactions sent by a user
     */
    List<Transaction> findByFromUserId(Long fromUserId);

    /**
     * Find all transactions related to a specific issue
     */
    List<Transaction> findByBountyIssueId(Long issueId);

    /**
     * Find all pending transactions (in escrow)
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Find a pending transaction for a specific issue
     */
    Optional<Transaction> findByBountyIssueIdAndStatus(Long issueId, TransactionStatus status);

    /**
     * Find all transactions for a user by status
     */
    List<Transaction> findByToUserIdAndStatus(Long toUserId, TransactionStatus status);

    /**
     * Find all transactions from a user by status
     */
    List<Transaction> findByFromUserIdAndStatus(Long fromUserId, TransactionStatus status);

    /**
     * Finds a pending payout for a bounty.
     */
    Optional<Transaction> findByBountyIdAndStatus(Long bountyId, TransactionStatus status);

    @Modifying
    @Query("""
    update Transaction t
    set t.bounty = null
    where t.bounty.id in (
        select b.id
        from Bounty b
        where b.issue.repository.id = :repositoryId
    )
""")
    int detachBountyReferencesForRepository(@Param("repositoryId") Long repositoryId);
}

