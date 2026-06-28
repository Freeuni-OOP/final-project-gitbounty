package org.gitbounty.gitbountybackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Let Spring Security handle 401/403 — re-throw so its filters respond correctly
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void handleSecurityExceptions(RuntimeException ex) {
        throw ex;
    }

    // --- 404 NOT FOUND HANDLERS ---
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandlerFound(NoHandlerFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.");
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            BranchNotFoundException.class,
            CodebaseNotFoundException.class,
            CodebaseMemberNotFoundException.class,
            BountyNotFoundException.class,
            IssueNotFoundException.class,
            PRNotFoundException.class,
            TransactionNotFoundException.class
    })
    public ResponseEntity<Object> handleNotFoundExceptions(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- 409 CONFLICT HANDLERS ---
    @ExceptionHandler({
            DuplicateUserException.class,
            DuplicateCodebaseMemberException.class,
            DuplicatePaymentRequestException.class,
            PRBranchesAreSameException.class
    })
    public ResponseEntity<Object> handleConflictExceptions(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Preserve the status code embedded in ResponseStatusException (e.g. 404, 409)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse((HttpStatus) ex.getStatusCode(), ex.getReason());
    }

    // --- 400 BAD REQUEST HANDLERS ---
    @ExceptionHandler({
            IllegalArgumentException.class,
            BountyAlreadyCompletedException.class
    })
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- 500 INTERNAL SERVER ERROR CATCH-ALL ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // Standardized error JSON builder
    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }
}