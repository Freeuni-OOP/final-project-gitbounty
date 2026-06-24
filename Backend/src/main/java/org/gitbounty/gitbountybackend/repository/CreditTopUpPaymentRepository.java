package org.gitbounty.gitbountybackend.repository;

import org.gitbounty.gitbountybackend.model.CreditTopUpPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditTopUpPaymentRepository extends JpaRepository<CreditTopUpPayment, Long> {
    List<CreditTopUpPayment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CreditTopUpPayment> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
