package org.gitbounty.gitbountybackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class GitAPIException extends RuntimeException {
    public GitAPIException(String message) {
        super(message);
    }
}
