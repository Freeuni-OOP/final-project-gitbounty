package org.gitbounty.gitbountybackend.exception;

public class BountyNotFoundException extends RuntimeException {
    public BountyNotFoundException(Long id) {
        super("Bounty not found with id: " + id);
    }
}