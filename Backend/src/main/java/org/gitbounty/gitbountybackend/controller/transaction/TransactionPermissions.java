package org.gitbounty.gitbountybackend.controller.transaction;

import org.gitbounty.gitbountybackend.model.Transaction;
import org.gitbounty.gitbountybackend.service.transaction.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("transactionPermissions")
public class TransactionPermissions {

    private final TransactionRepository transactionRepository;

    public TransactionPermissions(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Check whether the authenticated Keycloak ID (sub) is the creator (fromUser).
     */
    public boolean isCreator(Long transactionId, String authenticatedKeycloakId) {
        if (transactionId == null || authenticatedKeycloakId == null || authenticatedKeycloakId.isBlank()) {
            return false;
        }

        Optional<Transaction> tx = transactionRepository.findById(transactionId);
        return tx.map(t -> t.getFromUser() != null && authenticatedKeycloakId.equals(t.getFromUser().getKeycloakId()))
                .orElse(false);
    }

    /**
     * Check whether the authenticated Keycloak ID is involved in the transaction.
     */
    public boolean isInvolved(Long transactionId, String authenticatedKeycloakId) {
        if (transactionId == null || authenticatedKeycloakId == null || authenticatedKeycloakId.isBlank()) {
            return false;
        }

        Optional<Transaction> tx = transactionRepository.findById(transactionId);
        return tx.map(t -> {
            boolean isFrom = t.getFromUser() != null && authenticatedKeycloakId.equals(t.getFromUser().getKeycloakId());
            boolean isTo = t.getToUser() != null && authenticatedKeycloakId.equals(t.getToUser().getKeycloakId());
            return isFrom || isTo;
        }).orElse(false);
    }
}