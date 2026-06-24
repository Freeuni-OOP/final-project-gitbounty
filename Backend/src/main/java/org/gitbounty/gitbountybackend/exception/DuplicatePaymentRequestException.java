package org.gitbounty.gitbountybackend.exception;

public class DuplicatePaymentRequestException extends RuntimeException {
    public DuplicatePaymentRequestException(String message) {
        super(message);
    }
}