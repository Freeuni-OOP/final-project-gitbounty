package org.gitbounty.gitbountybackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MergeConflictException extends RuntimeException {
    public MergeConflictException(String message) {
        super(message);
    }
}
