package org.gitbounty.gitbountybackend.model;

public enum TransactionStatus {
    PENDING, // Awaiting approval in escrow
    COMPLETED, // Transaction completed successfully
    DISPUTED, // Transaction is under dispute
    REJECTED // Transaction was rejected
}
