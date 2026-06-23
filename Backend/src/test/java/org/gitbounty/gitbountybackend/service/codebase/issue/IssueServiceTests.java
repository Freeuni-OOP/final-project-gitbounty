package org.gitbounty.gitbountybackend.service.codebase.issue;

import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.exception.IssueNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTests {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CodebaseService codebaseService;

    @Mock
    private UserService userService;

    @InjectMocks
    private IssueService issueService;

    private Principal principal(String username) {
        return () -> username;
    }

    private User user(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@gitbounty.com");
        user.setKeycloakId("keycloak-" + username);
        return user;
    }

    private Codebase codebase(String name) {
        Codebase codebase = new Codebase();
        codebase.setId(10L);
        codebase.setName(name);
        codebase.setGitUrl("http://localhost/git/" + name + ".git");
        return codebase;
    }

    @Test
    void createIssueShouldSaveIssueSuccessfully() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.empty());
        when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue createdIssue = issueService.createIssue(
                "gitbounty-core",
                "Fix login bug",
                "Login fails with token",
                principal("tester")
        );

        assertEquals(1, createdIssue.getNumber());
        assertEquals("Fix login bug", createdIssue.getTitle());
        assertEquals("Login fails with token", createdIssue.getDescription());
        assertEquals(IssueStatus.OPEN, createdIssue.getStatus());
        assertEquals(author, createdIssue.getAuthor());
        assertEquals(codebase, createdIssue.getRepository());

        verify(userService).findByUsername("tester");
        verify(codebaseService).getCodebase("gitbounty-core");
        verify(issueRepository).findMaxNumberByRepositoryId(10L);
        verify(issueRepository).saveAndFlush(any(Issue.class));
    }

    @Test
    void createIssueShouldAssignNextIssueNumber() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.of(6));
        when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue createdIssue = issueService.createIssue(
                "gitbounty-core",
                "Fix login bug",
                "Login fails with token",
                principal("tester")
        );

        assertEquals(7, createdIssue.getNumber());

        verify(issueRepository).findMaxNumberByRepositoryId(10L);
        verify(issueRepository).saveAndFlush(any(Issue.class));
    }

    @Test
    void createIssueShouldTrimTitleAndDescription() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.empty());
        when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue createdIssue = issueService.createIssue(
                "gitbounty-core",
                "   Fix sidebar alignment   ",
                "   Sidebar overlaps content   ",
                principal("tester")
        );

        assertEquals("Fix sidebar alignment", createdIssue.getTitle());
        assertEquals("Sidebar overlaps content", createdIssue.getDescription());
    }

    @Test
    void createIssueShouldAllowNullDescription() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.empty());
        when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue createdIssue = issueService.createIssue(
                "gitbounty-core",
                "Fix issue without description",
                null,
                principal("tester")
        );

        assertNull(createdIssue.getDescription());
        assertEquals("Fix issue without description", createdIssue.getTitle());
    }

    @Test
    void createIssueShouldRejectNullTitle() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> issueService.createIssue(
                        "gitbounty-core",
                        null,
                        "Description",
                        principal("tester")
                )
        );

        assertEquals("Issue title is required", exception.getMessage());
        verifyNoInteractions(userService, codebaseService, issueRepository);
    }

    @Test
    void createIssueShouldRejectBlankTitle() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> issueService.createIssue(
                        "gitbounty-core",
                        "     ",
                        "Description",
                        principal("tester")
                )
        );

        assertEquals("Issue title is required", exception.getMessage());
        verifyNoInteractions(userService, codebaseService, issueRepository);
    }

    @Test
    void createIssueShouldRejectWhenAuthenticatedUserIsNotFound() {
        when(userService.findByUsername("missing-user")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> issueService.createIssue(
                        "gitbounty-core",
                        "Fix login bug",
                        "Description",
                        principal("missing-user")
                )
        );

        assertEquals("Authenticated user not found", exception.getMessage());
        verify(userService).findByUsername("missing-user");
        verifyNoInteractions(codebaseService, issueRepository);
    }

    @Test
    void createIssueShouldUseCodebaseServiceToResolveRepository() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findMaxNumberByRepositoryId(10L)).thenReturn(Optional.empty());
        when(issueRepository.saveAndFlush(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        issueService.createIssue(
                "gitbounty-core",
                "Fix login bug",
                "Description",
                principal("tester")
        );

        verify(codebaseService).getCodebase("gitbounty-core");
    }

    @Test
    void createIssueShouldNotSaveWhenCodebaseServiceThrowsException() {
        User author = user("tester");

        when(userService.findByUsername("tester")).thenReturn(Optional.of(author));
        when(codebaseService.getCodebase("missing-codebase"))
                .thenThrow(new CodebaseNotFoundException("Repository not found"));

        CodebaseNotFoundException exception = assertThrows(
                CodebaseNotFoundException.class,
                () -> issueService.createIssue(
                        "missing-codebase",
                        "Fix login bug",
                        "Description",
                        principal("tester")
                )
        );

        assertEquals("Repository not found", exception.getMessage());
        verify(userService).findByUsername("tester");
        verify(codebaseService).getCodebase("missing-codebase");
        verify(issueRepository, never()).saveAndFlush(any(Issue.class));
    }

    @Test
    void listIssuesShouldReturnIssuesForRepository() {
        Codebase codebase = codebase("gitbounty-core");

        Issue firstIssue = new Issue();
        firstIssue.setId(1L);
        firstIssue.setTitle("Fix login bug");
        firstIssue.setDescription("Login fails with token");
        firstIssue.setRepository(codebase);

        Issue secondIssue = new Issue();
        secondIssue.setId(2L);
        secondIssue.setTitle("Fix sidebar bug");
        secondIssue.setDescription("Sidebar overlaps content");
        secondIssue.setRepository(codebase);

        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findByRepositoryName("gitbounty-core"))
                .thenReturn(List.of(firstIssue, secondIssue));

        List<Issue> issues = issueService.listIssues("gitbounty-core");

        assertEquals(2, issues.size());
        assertEquals("Fix login bug", issues.get(0).getTitle());
        assertEquals("Fix sidebar bug", issues.get(1).getTitle());

        verify(codebaseService).getCodebase("gitbounty-core");
        verify(issueRepository).findByRepositoryName("gitbounty-core");
    }

    @Test
    void listIssuesShouldNotQueryIssuesWhenCodebaseDoesNotExist() {
        when(codebaseService.getCodebase("missing-codebase"))
                .thenThrow(new CodebaseNotFoundException("Repository not found"));

        CodebaseNotFoundException exception = assertThrows(
                CodebaseNotFoundException.class,
                () -> issueService.listIssues("missing-codebase")
        );

        assertEquals("Repository not found", exception.getMessage());
        verify(codebaseService).getCodebase("missing-codebase");
        verifyNoInteractions(issueRepository);
    }

    @Test
    void getIssueShouldReturnIssueByRepositoryNameAndNumber() {
        User author = user("tester");
        Codebase codebase = codebase("gitbounty-core");

        Issue issue = new Issue();
        issue.setId(1L);
        issue.setNumber(7);
        issue.setTitle("Fix login bug");
        issue.setDescription("Login fails with token");
        issue.setAuthor(author);
        issue.setRepository(codebase);

        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findByRepositoryNameAndNumber("gitbounty-core", 7))
                .thenReturn(Optional.of(issue));

        Issue result = issueService.getIssue("gitbounty-core", 7);

        assertEquals(7, result.getNumber());
        assertEquals("Fix login bug", result.getTitle());
        assertEquals(codebase, result.getRepository());

        verify(codebaseService).getCodebase("gitbounty-core");
        verify(issueRepository).findByRepositoryNameAndNumber("gitbounty-core", 7);
    }

    @Test
    void getIssueShouldThrowNotFoundWhenIssueDoesNotExist() {
        Codebase codebase = codebase("gitbounty-core");

        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);
        when(issueRepository.findByRepositoryNameAndNumber("gitbounty-core", 99))
                .thenReturn(Optional.empty());

        IssueNotFoundException exception = assertThrows(
                IssueNotFoundException.class,
                () -> issueService.getIssue("gitbounty-core", 99)
        );

        assertEquals("Issue not found: #99", exception.getMessage());
        verify(codebaseService).getCodebase("gitbounty-core");
        verify(issueRepository).findByRepositoryNameAndNumber("gitbounty-core", 99);
    }
}