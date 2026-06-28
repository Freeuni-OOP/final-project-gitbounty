package org.gitbounty.gitbountybackend.controller.issue;

import org.gitbounty.gitbountybackend.config.TestSecurityConfig;
import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IssueController.class)
@Import(TestSecurityConfig.class)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private CodebasePermissions codebasePermissions;

    private static User user(Long id, String username, String keycloakId) {
        User user = new User(username, username + "@test.com", keycloakId);
        user.setId(id);
        return user;
    }

    private static Codebase codebase(Long id, String name, User owner) {
        Codebase codebase = new Codebase();
        codebase.setId(id);
        codebase.setName(name);
        codebase.setOwner(owner);
        return codebase;
    }

    private static Issue issue(Long id, Integer number, String title, IssueStatus status) {
        User author = user(1L, "author", "kc-author");
        Codebase repository = codebase(10L, "my-repo", author);

        Issue issue = new Issue();
        issue.setId(id);
        issue.setNumber(number);
        issue.setTitle(title);
        issue.setDescription("Test issue description");
        issue.setStatus(status);
        issue.setAuthor(author);
        issue.setRepository(repository);
        issue.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        issue.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 30));
        return issue;
    }

    @Test
    void createIssue_ShouldReturnCreated_WhenAuthenticated() throws Exception {
        Issue created = issue(1L, 7, "Fix login bug", IssueStatus.OPEN);

        when(issueService.createIssue(eq("my-repo"), eq("Fix login bug"), eq("Test issue description"), any(Principal.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/codebases/my-repo/issues")
                        .with(jwt().jwt(builder -> builder.subject("kc-author")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Fix login bug",
                                "description": "Test issue description"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/codebases/my-repo/issues/7"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.description").value("Test issue description"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.repositoryId").value(10));

        verify(issueService).createIssue(eq("my-repo"), eq("Fix login bug"), eq("Test issue description"), any(Principal.class));
    }

    @Test
    void createIssue_ShouldReturnForbidden_WhenPrincipalMissing() throws Exception {
        mockMvc.perform(post("/api/codebases/my-repo/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Fix login bug",
                                "description": "Test issue description"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(issueService);
    }

    @Test
    void listIssues_ShouldReturnOk() throws Exception {
        Issue first = issue(1L, 1, "First issue", IssueStatus.OPEN);
        Issue second = issue(2L, 2, "Second issue", IssueStatus.CLOSED);

        when(issueService.listIssues("my-repo")).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/codebases/my-repo/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("First issue"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Second issue"))
                .andExpect(jsonPath("$[1].status").value("CLOSED"));

        verify(issueService).listIssues("my-repo");
    }

    @Test
    void getIssue_ShouldReturnOk() throws Exception {
        Issue found = issue(1L, 7, "Fix login bug", IssueStatus.OPEN);

        when(issueService.getIssue("my-repo", 7)).thenReturn(found);

        mockMvc.perform(get("/api/codebases/my-repo/issues/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(issueService).getIssue("my-repo", 7);
    }

    @Test
    void updateIssueState_ShouldReturnOk_WhenOwner() throws Exception {
        Issue closed = issue(1L, 7, "Fix login bug", IssueStatus.CLOSED);

        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(true);
        when(issueService.updateIssueState("my-repo", 7, IssueStatus.CLOSED)).thenReturn(closed);

        mockMvc.perform(patch("/api/codebases/my-repo/issues/7/state")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "CLOSED"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(codebasePermissions).isOwnerBySubject("my-repo", "kc-owner");
        verify(issueService).updateIssueState("my-repo", 7, IssueStatus.CLOSED);
    }

    @Test
    void updateIssueState_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-other")).thenReturn(false);

        mockMvc.perform(patch("/api/codebases/my-repo/issues/7/state")
                        .with(jwt().jwt(builder -> builder.subject("kc-other")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "CLOSED"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(codebasePermissions).isOwnerBySubject("my-repo", "kc-other");
        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_ShouldReturnForbidden_WhenPrincipalNameIsNull() throws Exception {
        mockMvc.perform(post("/api/codebases/my-repo/issues")
                        .principal(() -> null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "title": "Fix login bug",
                            "description": "Test issue description"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_ShouldReturnForbidden_WhenPrincipalNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/codebases/my-repo/issues")
                        .principal(() -> "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "title": "Fix login bug",
                            "description": "Test issue description"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(issueService);
    }

    @Test
    void updateIssueState_ShouldReturnForbidden_WhenJwtMissing() throws Exception {
        mockMvc.perform(patch("/api/codebases/my-repo/issues/7/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "status": "CLOSED"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(issueService);
        verifyNoInteractions(codebasePermissions);
    }
}