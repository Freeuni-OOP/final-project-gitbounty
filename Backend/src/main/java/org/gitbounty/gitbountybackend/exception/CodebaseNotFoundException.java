package org.gitbounty.gitbountybackend.exception;

public class CodebaseNotFoundException extends ResourceNotFoundException {
    public CodebaseNotFoundException(String message) {
        super(message);
    }
}
