package org.gitbounty.gitbountybackend.exception;

public class TransactionNotFoundException extends ResourceNotFoundException {
    public TransactionNotFoundException(Long id) {
        super("Transaction not found: " + id);
    }

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
