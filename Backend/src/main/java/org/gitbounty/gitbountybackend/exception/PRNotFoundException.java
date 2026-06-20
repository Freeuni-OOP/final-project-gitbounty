package org.gitbounty.gitbountybackend.exception;

public class PRNotFoundException extends ResourceNotFoundException {
    public PRNotFoundException(String message) {
        super(message);
    }
    public PRNotFoundException(Integer prNumber, String repositoryName) {
        super("Pull Request #"  + prNumber + " not found in: " + repositoryName);
    }
}
