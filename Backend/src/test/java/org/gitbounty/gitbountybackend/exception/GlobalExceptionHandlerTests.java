package org.gitbounty.gitbountybackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleUserNotFoundExceptionShouldReturnNotFound() {
        assertErrorResponse(
                globalExceptionHandler.handleNotFoundExceptions(new UserNotFoundException("User not found")),
                HttpStatus.NOT_FOUND,
                "User not found"
        );
    }

    @Test
    void handleBranchNotFoundExceptionShouldReturnNotFound() {
        assertErrorResponse(
                globalExceptionHandler.handleNotFoundExceptions(new BranchNotFoundException("Branch not found")),
                HttpStatus.NOT_FOUND,
                "Branch not found"
        );
    }

    @Test
    void handleCodebaseNotFoundExceptionShouldReturnNotFound() {
        assertErrorResponse(
                globalExceptionHandler.handleNotFoundExceptions(new CodebaseNotFoundException("Codebase not found")),
                HttpStatus.NOT_FOUND,
                "Codebase not found"
        );
    }

    @Test
    void handleCodebaseMemberNotFoundExceptionShouldReturnNotFound() {
        assertErrorResponse(
                globalExceptionHandler.handleNotFoundExceptions(new CodebaseMemberNotFoundException("Member not found")),
                HttpStatus.NOT_FOUND,
                "Member not found"
        );
    }

    @Test
    void handleIssueNotFoundExceptionShouldReturnNotFound() {
        assertErrorResponse(
                globalExceptionHandler.handleNotFoundExceptions(new IssueNotFoundException("Issue not found: #99")),
                HttpStatus.NOT_FOUND,
                "Issue not found: #99"
        );
    }

    @Test
    void handleDuplicateUserExceptionShouldReturnConflict() {
        assertErrorResponse(
                globalExceptionHandler.handleConflictExceptions(new DuplicateUserException("Duplicate user")),
                HttpStatus.CONFLICT,
                "Duplicate user"
        );
    }

    @Test
    void handleDuplicateCodebaseMemberExceptionShouldReturnConflict() {
        assertErrorResponse(
                globalExceptionHandler.handleConflictExceptions(new DuplicateCodebaseMemberException("Duplicate member")),
                HttpStatus.CONFLICT,
                "Duplicate member"
        );
    }

    @Test
    void handlePRBranchesAreSameExceptionShouldReturnConflict() {
        assertErrorResponse(
                globalExceptionHandler.handleConflictExceptions(new PRBranchesAreSameException("Branches are same")),
                HttpStatus.CONFLICT,
                "Branches are same"
        );
    }

    @Test
    void handleResponseStatusExceptionShouldPreserveStatusCodeAndMessage() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Bad request message"
        );

        assertErrorResponse(
                globalExceptionHandler.handleResponseStatusException(exception),
                HttpStatus.BAD_REQUEST,
                "Bad request message"
        );
    }

    @Test
    void handleIllegalArgumentExceptionShouldReturnBadRequest() {
        assertErrorResponse(
                globalExceptionHandler.handleIllegalArgumentException(new IllegalArgumentException("Invalid argument")),
                HttpStatus.BAD_REQUEST,
                "Invalid argument"
        );
    }

    @Test
    void handleGeneralExceptionShouldReturnInternalServerError() {
        assertErrorResponse(
                globalExceptionHandler.handleGeneralException(new RuntimeException("Unexpected error")),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error"
        );
    }

    @Test
    void handleDatabaseTransactionExceptionShouldReturnInternalServerError() {
        DatabaseTransactionException exception = mock(DatabaseTransactionException.class);
        when(exception.getMessage()).thenReturn("Database transaction failed");

        assertErrorResponse(
                globalExceptionHandler.handleGeneralException(exception),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database transaction failed"
        );
    }

    @Test
    void handleGitAPIExceptionShouldReturnInternalServerError() {
        GitAPIException exception = mock(GitAPIException.class);
        when(exception.getMessage()).thenReturn("Git API failed");

        assertErrorResponse(
                globalExceptionHandler.handleGeneralException(exception),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Git API failed"
        );
    }

    @Test
    void handleMergeConflictExceptionShouldReturnInternalServerError() {
        MergeConflictException exception = mock(MergeConflictException.class);
        when(exception.getMessage()).thenReturn("Merge conflict detected");

        assertErrorResponse(
                globalExceptionHandler.handleGeneralException(exception),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Merge conflict detected"
        );
    }

    @Test
    void handleAccessDeniedExceptionShouldRethrowException() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        AccessDeniedException thrown = assertThrows(
                AccessDeniedException.class,
                () -> globalExceptionHandler.handleSecurityExceptions(exception)
        );

        assertSame(exception, thrown);
    }

    @Test
    void handleAuthenticationExceptionShouldRethrowException() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        BadCredentialsException thrown = assertThrows(
                BadCredentialsException.class,
                () -> globalExceptionHandler.handleSecurityExceptions(exception)
        );

        assertSame(exception, thrown);
    }

    private void assertErrorResponse(
            ResponseEntity<Object> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertEquals(expectedStatus, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(expectedStatus.value(), body.get("status"));
        assertEquals(expectedStatus.getReasonPhrase(), body.get("error"));
        assertEquals(expectedMessage, body.get("message"));
    }
}