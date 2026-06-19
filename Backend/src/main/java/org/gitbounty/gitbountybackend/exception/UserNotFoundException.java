package org.gitbounty.gitbountybackend.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
