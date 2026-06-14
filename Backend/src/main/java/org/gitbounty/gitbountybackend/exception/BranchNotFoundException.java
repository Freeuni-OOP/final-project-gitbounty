package org.gitbounty.gitbountybackend.exception;

public class BranchNotFoundException extends ResourceNotFoundException {
    public BranchNotFoundException(String message) {
        super(message);
    }
}
