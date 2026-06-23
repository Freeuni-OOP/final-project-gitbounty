package org.gitbounty.gitbountybackend.exception;

public class BountyAlreadyCompletedException extends RuntimeException {
    public BountyAlreadyCompletedException(Long id) {
        super("Cannot cancel bounty with id " + id + " because it is already completed.");
    }
}