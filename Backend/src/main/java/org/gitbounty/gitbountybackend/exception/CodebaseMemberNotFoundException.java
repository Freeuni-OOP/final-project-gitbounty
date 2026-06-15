package org.gitbounty.gitbountybackend.exception;

public class CodebaseMemberNotFoundException extends RuntimeException {

    public CodebaseMemberNotFoundException(String message) {
        super(message);
    }
}
