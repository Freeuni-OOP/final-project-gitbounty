package org.gitbounty.gitbountybackend.exception;

public class CodebaseNotFoundException extends RuntimeException {
    public CodebaseNotFoundException(String message) {
        super(message);
    }
}
