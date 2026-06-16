package org.gitbounty.gitbountybackend.exception;

public class DuplicateCodebaseMemberException extends RuntimeException {

    public DuplicateCodebaseMemberException(String message) {
        super(message);
    }
}
